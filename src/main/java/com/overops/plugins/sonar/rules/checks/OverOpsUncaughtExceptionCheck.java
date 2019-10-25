package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

@Rule(key = "UncaughtException",
        name = "UncaughtException",
        description = "Over ops description",
        tags = {"over-ops", "oo-uncaught-exception"})
public class OverOpsUncaughtExceptionCheck extends OverOpsBaseException {

    public OverOpsUncaughtExceptionCheck() {
        ooExceptionType = "UncaughtException";
        issueMessagePrefix = "UncaughtException";
    }
}
