package com.overops.plugins.sonar.config;

import org.junit.jupiter.api.Test;
import org.sonar.api.measures.Metric;

import java.util.List;

import static com.overops.plugins.sonar.config.OverOpsMetrics.NEW;
import static com.overops.plugins.sonar.config.OverOpsMetrics.CRITICAL;
import static com.overops.plugins.sonar.config.OverOpsMetrics.RESURFACED;
import static com.overops.plugins.sonar.config.OverOpsMetrics.UNIQUE;
import static org.assertj.core.api.Assertions.assertThat;

class OverOpsMetricsTest {

    @Test
    void getMetrics(){
        OverOpsMetrics overOpsMetrics = new OverOpsMetrics();
        List<Metric> metrics = overOpsMetrics.getMetrics();

        assertThat(metrics).containsExactly(NEW, CRITICAL, RESURFACED, UNIQUE);
    }
}
