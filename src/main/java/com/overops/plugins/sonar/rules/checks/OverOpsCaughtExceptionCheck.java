package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.CAUGHT_EXCEPTION;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.CAUGHT_EXCEPTION_RULE_KEY;

@Rule(key = CAUGHT_EXCEPTION_RULE_KEY)
public class OverOpsCaughtExceptionCheck extends OverOpsBaseException {
    public OverOpsCaughtExceptionCheck() {
        this.metric = CAUGHT_EXCEPTION;
    }
}