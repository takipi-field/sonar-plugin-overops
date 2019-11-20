package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.INCREASING_QG_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.INCREASING_QG_METRIC;

@Rule(key = INCREASING_QG_KEY)
public class OverOpsIncreasingQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsIncreasingQualityGateCheck() {
        this.metric = INCREASING_QG_METRIC;
    }
}