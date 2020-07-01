package com.overops.plugins.sonar;

import com.overops.plugins.sonar.config.OverOpsMetrics;
import com.overops.plugins.sonar.config.OverOpsRulesDefinition;
import com.overops.plugins.sonar.impl.TestSonarRuntime;
import org.junit.jupiter.api.Test;
import org.sonar.api.*;

import static org.assertj.core.api.Assertions.assertThat;

public class OverOpsPluginTest {

    @Test
    void test(){
        OverOpsPlugin plugin = new OverOpsPlugin();
        Plugin.Context context = new Plugin.Context(new TestSonarRuntime());
        plugin.define(context);

        assertThat(context.getExtensions()).contains(OverOpsRulesDefinition.class);
        assertThat(context.getExtensions()).contains(OverOpsMetrics.class);
        assertThat(context.getExtensions()).contains(JavaEventSensor.class);
        assertThat(context.getExtensions()).contains(DotNetEventSensor.class);
        assertThat(context.getExtensions()).contains(EventsMetricComputer.class);
        assertThat(context.getExtensions()).contains(AddArcComment.class);
    }
}
