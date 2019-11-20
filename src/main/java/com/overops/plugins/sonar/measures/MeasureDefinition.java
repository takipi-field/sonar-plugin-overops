package com.overops.plugins.sonar.measures;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.overops.plugins.sonar.OverOpsPlugin.overOpsEventsStatistic;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.getMetricByQualityGate;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.getOverOpsByQualityGate;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.getMetricsList;

public class MeasureDefinition implements MeasureComputer {
    private static final Logger log = Loggers.get(MeasureDefinition.class);

    @Override
    public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
        List<String> collect = getMetricsList().stream().map(metric -> metric.key()).collect(Collectors.toList());
        return def.newDefinitionBuilder()
                .setOutputMetrics(collect.toArray(new String[collect.size()]))
                .build();
    }


    @Override
    public void compute(MeasureComputerContext context) {
        if (context.getComponent().getType() == Component.Type.FILE) {
            String filePathJavaStyle = context.getComponent().getKey().replaceAll("/", ".");
            overOpsEventsStatistic.getStatistic()
                    .stream()
                    .filter(classStat -> {
                        System.out.println("");
                        System.out.println("component that scan path " + filePathJavaStyle);
                        System.out.println("classStat.fileName " + classStat.fileName);
                        return filePathJavaStyle.indexOf(classStat.fileName) != -1;})
                    .collect(Collectors.toList())
                    .forEach(classStat -> classStat.qualityGateToEventStat.forEach((qualityGate, eventInClassStat) -> {
                                OverOpsMetrics.OverOpsMetric overOpsMetric = getOverOpsByQualityGate(qualityGate);
                                System.out.println("        qualityGate [" + qualityGate + "]  overOpsMetric for it found " + (overOpsMetric != null));
                                if (overOpsMetric != null) {
                                    //If in the same line we had several times event occurs we count it once
                                    if (overOpsMetric.isCombo()) {
                                        for (String gate :overOpsMetric.qualityGate){
                                            Metric metric = getMetricByQualityGate(gate);
                                            if (metric != null) {
                                                context.addMeasure(metric.getKey(), eventInClassStat.lineToLineStat.keySet().size());
                                            }
                                        }
                                    } else {
                                        context.addMeasure(overOpsMetric.metric.getKey(), eventInClassStat.lineToLineStat.keySet().size());
                                    }
                                }
                            })
                    );
        }

        if (context.getComponent().getType() != Component.Type.FILE) {
            getMetricsList().stream().forEach(metric -> {
                int sum = StreamSupport.stream(context.getChildrenMeasures(metric.key()).spliterator(), false)
                        .mapToInt(Measure::getIntValue)
                        .sum();
                context.addMeasure(metric.key(), sum);
            });
        }
    }
}
