package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.UNCAUGHT_EXCEPTION_RULE_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.UNCAUGHT_EXCEPTION;

@Rule(key = UNCAUGHT_EXCEPTION_RULE_KEY)
public class OverOpsUncaughtExceptionCheck extends OverOpsBaseExceptionCheck {
    public OverOpsUncaughtExceptionCheck() {
        this.metric = UNCAUGHT_EXCEPTION;
    }
}
