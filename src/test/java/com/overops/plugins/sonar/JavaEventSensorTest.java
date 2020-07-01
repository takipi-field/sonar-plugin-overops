package com.overops.plugins.sonar;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;

import static com.overops.plugins.sonar.config.OverOpsRulesDefinition.JAVA_LANGUAGE;
import static com.overops.plugins.sonar.config.OverOpsRulesDefinition.JAVA_REPOSITORY;
import static org.assertj.core.api.Assertions.assertThat;

class JavaEventSensorTest extends AbstractEventSensorTest {

	@Test
	void testDescribe(){
		DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
		defineSensor().describe(descriptor);

		assertThat(descriptor.name()).isEqualTo("OverOps Event Issues Sensor for Java");
		assertThat(descriptor.languages()).containsExactly(JAVA_LANGUAGE);
		assertThat(descriptor.ruleRepositories()).containsExactly(JAVA_REPOSITORY);
	}

	@Override
	public EventSensor defineSensor() {
		return new JavaEventSensor();
	}
}