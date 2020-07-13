package com.overops.plugins.sonar;

import com.overops.plugins.sonar.config.OverOpsRulesDefinition;
import com.overops.plugins.sonar.config.OverOpsMetrics;
import com.overops.plugins.sonar.config.Properties;

import org.sonar.api.Plugin;

public class OverOpsPlugin implements Plugin {

	@Override
	public void define(Context context) {

		// plugin settings
		context.addExtensions(Properties.getProperties());

		// define rules for issues
		context.addExtension(OverOpsRulesDefinition.class);

		// analyzer - add metrics and issues
		context.addExtensions(OverOpsMetrics.class, JavaEventSensor.class, DotNetEventSensor.class, EventsMetricComputer.class);

		// post job - add ARC links to issues as comments
		context.addExtension(AddArcComment.class);
	}
}
