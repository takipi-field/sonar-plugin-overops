package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.CRITICAL_QG_METRIC;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.CRITICAL_QG_KEY;

@Rule(key = CRITICAL_QG_KEY)
public class OverOpsCriticalQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsCriticalQualityGateCheck() {
        this.metric = CRITICAL_QG_METRIC;
    }
}