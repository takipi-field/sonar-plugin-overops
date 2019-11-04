package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.SWALLOWED_EXCEPTION_RULE_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.SWALLOWED_EXCEPTION;

@Rule(key = SWALLOWED_EXCEPTION_RULE_KEY)
public class OverOpsSwallowedExceptionCheck extends OverOpsBaseExceptionCheck {
    public OverOpsSwallowedExceptionCheck() {
        this.metric = SWALLOWED_EXCEPTION;
    }
}
