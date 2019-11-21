package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.INCREASING_CRITICAL_QG_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.INCREASING_CRITICAL_QG_METRIC;

@Rule(key = INCREASING_CRITICAL_QG_KEY)
public class OverOpsIncreasingCriticalQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsIncreasingCriticalQualityGateCheck() {
        this.metric = INCREASING_CRITICAL_QG_METRIC;
    }
}