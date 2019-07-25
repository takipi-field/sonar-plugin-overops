package org.sonarsource.plugins.OverOps.converter;

import org.sonar.api.config.Configuration;
import org.sonarsource.plugins.OverOps.settings.OverOpsProperties;
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
	
	public String envId() {
		return configuration.get(OverOpsProperties.OO_ENVID).orElse(null);
	}
	
	public String appName() {
		return configuration.get(OverOpsProperties.APP_NAME).orElse(null);
	}
	
	public String depName() {
		return configuration.get(OverOpsProperties.DEP_NAME).orElse(null);
	}
	
	public int totalErrVolumeGate() {
		return Integer.parseInt(configuration.get(OverOpsProperties.TOTAL_ERROR_VOLUME_GATE).orElse(null));
	}
	
	public String eventId() {
		return configuration.get(OverOpsProperties.EVENTID).orElse(null);
	}
	
	

}
