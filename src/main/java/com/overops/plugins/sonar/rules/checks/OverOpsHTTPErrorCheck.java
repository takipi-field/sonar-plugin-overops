package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.HTTP_ERROR_RULE_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.HTTP_ERROR;

@Rule(key = HTTP_ERROR_RULE_KEY)
public class OverOpsHTTPErrorCheck extends OverOpsBaseExceptionCheck {
    public OverOpsHTTPErrorCheck() {
        this.metric = HTTP_ERROR;
    }
}
