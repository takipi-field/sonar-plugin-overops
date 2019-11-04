package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.CUSTOM_EVENT;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.CUSTOM_EVENT_RULE_KEY;

@Rule(key = CUSTOM_EVENT_RULE_KEY)
public class OverOpsCustomEventCheck extends OverOpsBaseExceptionCheck {
    public OverOpsCustomEventCheck() {
        this.metric = CUSTOM_EVENT;
    }
}
