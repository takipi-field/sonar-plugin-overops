package com.overops.plugins.sonar;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;

import static com.overops.plugins.sonar.config.OverOpsRulesDefinition.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DotNetEventSensorTest extends AbstractEventSensorTest {

	@Test
	void testDescribe(){
		DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
		defineSensor().describe(descriptor);

		assertThat(descriptor.name()).isEqualTo("OverOps Event Issues Sensor for .NET");
		assertThat(descriptor.languages()).containsExactly(CS_LANGUAGE);
		assertThat(descriptor.ruleRepositories()).containsExactly(CS_REPOSITORY);
	}

	@Override
	public EventSensor defineSensor() {
		return new DotNetEventSensor();
	}
}