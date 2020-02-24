package com.overops.plugins.sonar.impl;

import java.io.Serializable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.measure.NewMeasure;

public class TestNewMeasure<G extends Serializable> implements NewMeasure<G> {

	@Override
	public NewMeasure<G> on(InputComponent component) {
		return this;
	}

	@Override
	public NewMeasure<G> forMetric(Metric<G> metric) {
		return this;
	}

	@Override
	public NewMeasure<G> withValue(G value) {
		return this;
	}

	@Override
	public void save() {
		// do nothing
	}

}
