package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.NEW_QUALITY_GATE_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.NEW_QUALITY_GATE;

@Rule(key = NEW_QUALITY_GATE_KEY)
public class OverOpsNewQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsNewQualityGateCheck() {
        this.metric = NEW_QUALITY_GATE;
    }
}