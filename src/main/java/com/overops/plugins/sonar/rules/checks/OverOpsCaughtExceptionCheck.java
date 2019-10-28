package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.CAUGHT_EXCEPTION;

@Rule(key = "CaughtException",
        name = "CaughtException",
        description = "Over ops description",
        tags = {"over-ops", "oo-caught-exception"})
public class OverOpsCaughtExceptionCheck extends OverOpsBaseException {
    public OverOpsCaughtExceptionCheck() {
        this.metric = CAUGHT_EXCEPTION;
    }
}