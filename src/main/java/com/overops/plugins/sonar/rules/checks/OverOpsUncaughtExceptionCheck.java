package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.UNCAUGHT_EXCEPTION;

@Rule(key = "UncaughtException",
        name = "UncaughtException",
        description = "Over ops description",
        tags = {"over-ops", "oo-uncaught-exception"})
public class OverOpsUncaughtExceptionCheck extends OverOpsBaseException {
    public OverOpsUncaughtExceptionCheck() {
        this.metric = UNCAUGHT_EXCEPTION;
    }
}
