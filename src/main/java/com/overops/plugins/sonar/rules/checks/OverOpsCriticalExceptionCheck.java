package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.CRITICAL_EXCEPTION;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.CRITICAL_EXCEPTION_RULE_KEY;

@Rule(key = CRITICAL_EXCEPTION_RULE_KEY)
public class OverOpsCriticalExceptionCheck extends OverOpsBaseExceptionCheck {
    public OverOpsCriticalExceptionCheck() {
        this.metric = CRITICAL_EXCEPTION;
    }
}

