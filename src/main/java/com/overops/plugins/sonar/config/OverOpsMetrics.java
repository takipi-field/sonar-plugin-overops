package com.overops.plugins.sonar.config;

import java.util.List;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static java.util.Arrays.asList;

/**
 * Sonar Metrics Configuration
 */
public class OverOpsMetrics implements Metrics {

	public static final String OVEROPS = "OverOps";

	public static final Metric<Integer> NEW = new Metric.Builder("overops_new_errors","New Errors", Metric.ValueType.INT)
		.setDescription("New Errors")
		.setDirection(Metric.DIRECTION_WORST)
		.setQualitative(true)
		.setDomain(OVEROPS)
		.setBestValue(0.0)
		.create();

	public static final Metric<Integer> CRITICAL = new Metric.Builder("overops_critical_errors","Critical Errors", Metric.ValueType.INT)
		.setDescription("Critical Errors")
		.setDirection(Metric.DIRECTION_WORST)
		.setQualitative(true)
		.setDomain(OVEROPS)
		.setBestValue(0.0)
		.create();

	public static final Metric<Integer> RESURFACED = new Metric.Builder("overops_resurfaced_errors","Resurfaced Errors", Metric.ValueType.INT)
		.setDescription("Resurfaced Errors")
		.setDirection(Metric.DIRECTION_WORST)
		.setQualitative(true)
		.setDomain(OVEROPS)
		.setBestValue(0.0)
		.create();

	public static final Metric<Integer> UNIQUE = new Metric.Builder("overops_unique_errors","Unique Errors", Metric.ValueType.INT)
		.setDescription("Unique Errors")
		.setDirection(Metric.DIRECTION_WORST)
		.setQualitative(true)
		.setDomain(OVEROPS)
		.setBestValue(0.0)
		.create();

	@Override
	public List<Metric> getMetrics() {
		return asList(NEW, CRITICAL, RESURFACED, UNIQUE);
	}
}