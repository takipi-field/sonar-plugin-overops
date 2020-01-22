package com.overops.plugins.sonar.rules.checks;

import org.sonar.check.Rule;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.NEW_CRITICAL_QG_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.Constants.NEW_QG_KEY;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.NEW_CRITICAL_QG_METRIC;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.NEW_QG_METRIC;

@Rule(key = NEW_CRITICAL_QG_KEY)
public class OverOpsNewCriticalQualityGateCheck extends OverOpsBaseExceptionCheck {
    public OverOpsNewCriticalQualityGateCheck() {
        this.metric = NEW_CRITICAL_QG_METRIC;
    }
}