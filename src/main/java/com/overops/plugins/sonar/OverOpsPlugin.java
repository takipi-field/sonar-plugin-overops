package com.overops.plugins.sonar;

import com.overops.plugins.sonar.measures.MeasureDefinition;
import com.overops.plugins.sonar.measures.OverOpsMetrics;
import com.overops.plugins.sonar.rules.RuleDefinitionImplementation;
import com.overops.plugins.sonar.settings.OverOpsProperties;
import org.sonar.api.Plugin;

public class OverOpsPlugin implements Plugin {

    @Override
    public void define(Context context) {
        context.addExtension(RuleDefinitionImplementation.class);
        context.addExtensions(OverOpsProperties.getProperties());
        context.addExtension(AddCommentsPostJob.class);
        context.addExtensions(OverOpsMetrics.class, MeasureDefinition.class);
    }
}
