package com.overops.plugins.sonar;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;

import static com.overops.plugins.sonar.OverOpsMetrics.*;

public class EventsMetricComputer implements MeasureComputer {

	@Override
	public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
		return def.newDefinitionBuilder()
			.setOutputMetrics(NEW.key(), CRITICAL.key(), RESURFACED.key(), UNIQUE.key())
			.build();
	}

	@Override
	public void compute(MeasureComputerContext context) {
		// measure is defined on files by {@link EventsSensor}
		// sum up the totals for each file to enable code view drilldown
		if (context.getComponent().getType() != Component.Type.FILE) {
			int newSum = 0;
			int criticalSum = 0;
			int resurfacedSum = 0;
			int uniqueSum = 0;

			for (Measure child : context.getChildrenMeasures(NEW.key())) {
				newSum += child.getIntValue();
			}

			for (Measure child : context.getChildrenMeasures(CRITICAL.key())) {
				criticalSum += child.getIntValue();
			}

			for (Measure child : context.getChildrenMeasures(RESURFACED.key())) {
				resurfacedSum += child.getIntValue();
			}

			for (Measure child : context.getChildrenMeasures(UNIQUE.key())) {
				uniqueSum += child.getIntValue();
			}

			// set each folder's value to the sum of its files
			context.addMeasure(NEW.key(), newSum);
			context.addMeasure(CRITICAL.key(), criticalSum);
			context.addMeasure(RESURFACED.key(), resurfacedSum);
			context.addMeasure(UNIQUE.key(), uniqueSum);
		}
	}
}