package com.overops.plugins.sonar.config;

import static com.overops.plugins.sonar.config.Properties.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

public class PropertiesTest {

	@Test
	public void getProperties() {
		List<PropertyDefinition> properties = Properties.getProperties();
		assertThat(properties).containsExactly(PROP_API_URL,
				PROP_APP_URL,
				PROP_API_KEY,
				PROP_ENVIRONMENT_ID,
				PROP_APPLICATION_NAME,
				PROP_DEPLOYMENT_NAME,
				PROP_CRITICAL_EXCEPTION_TYPES,
				PROP_IGNORE_EVENT_TYPES);

	}
}