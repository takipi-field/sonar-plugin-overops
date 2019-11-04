package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.LOGGED_ERROR_RULE_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.LOGGED_ERROR;

@Rule(key = LOGGED_ERROR_RULE_KEY)
public class OverOpsLoggedErrorCheck extends OverOpsBaseExceptionCheck {
    public OverOpsLoggedErrorCheck() {
        this.metric = LOGGED_ERROR;
    }
}

