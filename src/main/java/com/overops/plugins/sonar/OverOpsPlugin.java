package com.overops.plugins.sonar;

import org.sonar.api.Plugin;

public class OverOpsPlugin implements Plugin {

	@Override
	public void define(Context context) {

		// plugin settings
		context.addExtensions(Properties.getProperties());

		// define rules for issues
		context.addExtension(JavaRulesDefinition.class);

		// analyzer - add metrics and issues
		context.addExtensions(OverOpsMetrics.class, EventsSensor.class, EventsMetricComputer.class);

		// post job - add ARC links to issues as comments
		context.addExtension(OverOpsPostJob.class);
	}
}
