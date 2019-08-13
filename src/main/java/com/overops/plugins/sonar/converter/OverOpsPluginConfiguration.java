package com.overops.plugins.sonar.converter;

import com.overops.plugins.sonar.settings.OverOpsProperties;

import org.sonar.api.config.Configuration;
public class OverOpsPluginConfiguration {
	
	private final Configuration configuration;

	public OverOpsPluginConfiguration(Configuration config) {
		configuration = config;
	}
	
	public String overopsURL() {
		return configuration.get(OverOpsProperties.OO_URL).orElse(null);
	}
	
	public String ooAPIKey() {
		return configuration.get(OverOpsProperties.APIKEY).orElse(null);
	}
		
	public String appName() {
		return configuration.get(OverOpsProperties.APP_NAME).orElse(null);
	}
	
	public String depName() {
		return configuration.get(OverOpsProperties.DEP_NAME).orElse(null);
	}

	public int days(){
		return configuration.getInt(OverOpsProperties.DAYS).orElse(1);
	}
}
