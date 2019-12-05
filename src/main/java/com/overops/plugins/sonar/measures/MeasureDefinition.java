package com.overops.plugins.sonar.measures;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.overops.plugins.sonar.OverOpsPlugin.getJavaStyleFilePath;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.getOverOpsByQualityGate;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.getMetricsList;

public class MeasureDefinition implements MeasureComputer {
    private OverOpsEventsStatistic overOpsEventsStatistic;

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
            String javaStyleFilePath = getJavaStyleFilePath(context.getComponent().getKey());
            int endingIndex = javaStyleFilePath.length() - ".java".length();
            readOverOpsEventsStatistic();
            if (overOpsEventsStatistic == null) {
                return;
            }

            overOpsEventsStatistic.getStatistic()
                    .stream()
                    .filter(classStat -> javaStyleFilePath.indexOf(classStat.fileName) == endingIndex - classStat.fileName.length())
                    .collect(Collectors.toList())
                    .forEach(classStat -> {
                        classStat.reportableQualityGateToEventStat.forEach((qualityGateKey, eventInClassStat) -> {
                            OverOpsMetrics.OverOpsMetric overOpsMetric = getOverOpsByQualityGate(qualityGateKey);
                            if (overOpsMetric != null) {
                                //If in the same line we had several times event occurs we count it once
                                context.addMeasure(overOpsMetric.metric.getKey(), eventInClassStat.lineToLineStat.keySet().size());
                            }
                        });
                    });
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

    private void readOverOpsEventsStatistic() {
        if (overOpsEventsStatistic != null) {
            return;
        }

        try {
            FileInputStream file = new FileInputStream("too.txt");
            ObjectInputStream in = new ObjectInputStream(file);

            overOpsEventsStatistic = (OverOpsEventsStatistic) in.readObject();

            in.close();
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
