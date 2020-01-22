package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.RESURFACED_CRITICAL_QG_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.RESURFACED_CRITICAL_QG_METRIC;

@Rule(key = RESURFACED_CRITICAL_QG_KEY)
public class OverOpsResurfacedCriticalQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsResurfacedCriticalQualityGateCheck() {
        this.metric = RESURFACED_CRITICAL_QG_METRIC;
    }
}