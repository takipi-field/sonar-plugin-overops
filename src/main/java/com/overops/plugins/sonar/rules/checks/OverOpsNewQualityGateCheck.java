package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.NEW_QG_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.NEW_QG_METRIC;

@Rule(key = NEW_QG_KEY)
public class OverOpsNewQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsNewQualityGateCheck() {
        this.metric = NEW_QG_METRIC;
    }
}