package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.RESURFACED_QG_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.RESURFACED_QG_METRIC;

@Rule(key = RESURFACED_QG_KEY)
public class OverOpsResurfacedQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsResurfacedQualityGateCheck() {
        this.metric = RESURFACED_QG_METRIC;
    }
}