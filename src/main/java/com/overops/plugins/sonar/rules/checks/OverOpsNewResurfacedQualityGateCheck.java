package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.NEW_RESURFACED_QG_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.NEW_RESURFACED_QG_METRIC;

@Rule(key = NEW_RESURFACED_QG_KEY)
public class OverOpsNewResurfacedQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsNewResurfacedQualityGateCheck() {
        this.metric = NEW_RESURFACED_QG_METRIC;
    }
}