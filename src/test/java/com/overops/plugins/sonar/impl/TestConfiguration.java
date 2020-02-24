package com.overops.plugins.sonar.impl;

import static com.overops.plugins.sonar.config.Properties.*;

import java.util.HashMap;
import java.util.Optional;
import org.sonar.api.config.Configuration;

public class TestConfiguration implements Configuration {

	public HashMap<String, String> config = new HashMap<>();

	public TestConfiguration() {
		config.put(API_URL, "http://localhost");
		config.put(APP_URL, "http://localhost");
		config.put(API_KEY, "test_key");
		config.put(ENVIRONMENT_ID, "S1234");
		config.put(APPLICATION_NAME, "app_name");
		config.put(DEPLOYMENT_NAME, "dep_name");
		config.put(CRITICAL_EXCEPTION_TYPES, DEFAULT_CRITICAL_EXCEPTION_TYPES);
		config.put("sonar.login", "test");
	}

	@Override
	public Optional<String> get(String key) {

		if (config.containsKey(key)) {
			return Optional.of(config.get(key));
		}

		return Optional.ofNullable(null);
	}

	@Override
	public boolean hasKey(String key) {
		return false;
	}

	@Override
	public String[] getStringArray(String key) {
		return null;
	}

}
