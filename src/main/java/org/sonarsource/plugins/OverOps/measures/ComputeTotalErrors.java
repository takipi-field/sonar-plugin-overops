package org.sonarsource.plugins.OverOps.measures;

import static org.sonarsource.plugins.OverOps.measures.OverOpsMetrics.FILENAME_SIZE;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import static org.sonarsource.plugins.OverOps.measures.OverOpsMetrics.SUMVIEW;

import java.util.Iterator;

import com.takipi.api.client.result.event.EventResult;

import static org.sonarsource.plugins.OverOps.measures.OOSensor.eventList;

public class ComputeTotalErrors implements MeasureComputer {

    @Override
    public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
        return defContext.newDefinitionBuilder().setInputMetrics(FILENAME_SIZE.key())
                .setInputMetrics(SUMVIEW.key()).build();
    }

    @Override
    public void compute(MeasureComputerContext context) {
        context.addMeasure(SUMVIEW.key(), OOSensor.eventList.events.size());
    }

}