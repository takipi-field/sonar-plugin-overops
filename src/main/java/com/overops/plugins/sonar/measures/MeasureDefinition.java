package com.overops.plugins.sonar.measures;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.Metric;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.overops.plugins.sonar.OverOpsPlugin.overOpsEventsStatistic;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.getMetric;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.getMetricsList;

public class MeasureDefinition implements MeasureComputer {
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
                    .filter(classStat -> filePathJavaStyle.indexOf(classStat.fileName) != -1)
                    .collect(Collectors.toList())
                    .forEach(classStat -> classStat.typeToEventStat.forEach((type, eventInClassStat) -> {
                                Metric metric = getMetric(type);
                                if (metric != null) {
                                    context.addMeasure(metric.getKey(), eventInClassStat.total);
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
