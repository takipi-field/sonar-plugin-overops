/*
 * Example Plugin for SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.overops.plugins.sonar;

import com.overops.plugins.sonar.measures.OverOpsEventsStatistic;
import com.overops.plugins.sonar.measures.MeasureDefinition;
import com.overops.plugins.sonar.measures.OverOpsMetrics;
import com.overops.plugins.sonar.rules.RuleDefinitionImplementation;
import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.sonar.api.Plugin;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.HashMap;

public class OverOpsPlugin implements Plugin {
	private static final Logger LOGGER = Loggers.get(OverOpsPlugin.class);
	public static final long DEFAULT_SPAN_DAYS = 1L;
	public static final String DEFAULT_VIEWID = "All Exceptions";
	public static final String DEFAULT_OVER_OPS_API_HOST = "https://api.overops.com";
	public static final String SONAR_HOST_PROPERTY = "sonar.host.url";
	public static String SONAR_HOST_URL;
	public static String AUTH_DATA;

	public static String environmentKey;
	public static String appHost;
	public static String apiKey;
	public static String deploymentName;
	public static String applicationName;
	public static DateTime to;
	public static DateTime from;
	public static DateTimeFormatter formatter;
	public static RemoteApiClient apiClient;
	public static long daysSpan;
	public static EventsResult volumeResult;
	public static OverOpsEventsStatistic overOpsEventsStatistic;

	public HashMap<String, Integer> exceptions;
	private String viewId;

	@Override
	public void define(Context context) {
		getOverOpsDataAndCreateStatistic(context);

		context.addExtension(RuleDefinitionImplementation.class);
		context.addExtensions(OverOpsMetrics.class, MeasureDefinition.class);
		context.addExtensions(OverOpsProperties.getProperties());
		context.addExtension(AddCommentsPostJob.class);
	}

	private boolean getOverOpsDataAndCreateStatistic(Context context) {
		if (overOpsEventsStatistic != null) {
			return true;
		}

		overOpsEventsStatistic = new OverOpsEventsStatistic();

		Configuration config = context.getBootConfiguration();
		getConfigProperties(config);

		if (!validateConfigData()) {
			return false;
		}

		apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(apiKey).setHostname(appHost).build();
		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, environmentKey, viewId);
		if (view == null) {
			LOGGER.error("Failed getting OverOps View, please check viewId or environmentKey.");
			return false;
		}

		UrlClient.Response<EventsResult> volumeResponse = getVolumeResponse(view);

		if (!validateVolumeResponse(volumeResponse)) {
			return false;
		}

		volumeResult = volumeResponse.data;

		for (EventResult event : volumeResult.events) {
			overOpsEventsStatistic.add(event);
		}

		return true;
	}


	private UrlClient.Response<EventsResult> getVolumeResponse(SummarizedView view) {
		EventsVolumeRequest eventsVolumeRequest = getVolumeRequest(view);
		return apiClient.get(eventsVolumeRequest);
	}

	private void getConfigProperties(Configuration config) {
		SONAR_HOST_URL = config.get(SONAR_HOST_PROPERTY).orElse("No host found");
		environmentKey = config.get(OverOpsProperties.SONAR_OVEROPS_ENVIRONMENT_ID).orElse(null);
		appHost = config.get(OverOpsProperties.SONAR_OVEROPS_API_HOST).orElse(DEFAULT_OVER_OPS_API_HOST);
		apiKey = config.get(OverOpsProperties.SONAR_OVEROPS_API_KEY).orElse(null);
		deploymentName = config.get(OverOpsProperties.SONAR_OVEROPS_DEP_NAME).orElse(null);
		applicationName = config.get(OverOpsProperties.SONAR_OVEROPS_APP_NAME).orElse(null);
		daysSpan = config.getLong(OverOpsProperties.SONAR_OVEROPS_SPAN_DAYS).orElse(DEFAULT_SPAN_DAYS);
		viewId = config.get(OverOpsProperties.SONAR_OVEROPS_VIEW_ID).orElse(DEFAULT_VIEWID);

		String userName = config.get(OverOpsProperties.SONAR_OVEROPS_USER_NAME).orElse("admin");
		String userPassword = config.get(OverOpsProperties.SONAR_OVEROPS_USER_PASSWORD).orElse("admin");
		AUTH_DATA = Base64.encode(new StringBuilder()
				.append(userName).append(":").append(userPassword)
				.toString().getBytes());

		to = DateTime.now();
		from = to.minusDays((int) daysSpan);
		formatter = ISODateTimeFormat.dateTime().withZoneUTC();

		logConfigData();
	}

	private boolean validateConfigData() {
		if (StringUtils.isEmpty(apiKey) ||
				StringUtils.isEmpty(environmentKey) ||
				StringUtils.isEmpty(appHost) ||
				StringUtils.isEmpty(viewId)) {
			LOGGER.error("Failed to process OverOps sensor one of [apiKey, environmentKey, appHost, viewId] properties is empty. Please check project OverOps configuration.");
			return false;
		}

		return true;
	}

	private EventsVolumeRequest getVolumeRequest(SummarizedView view) {
		EventsVolumeRequest.Builder builder = EventsVolumeRequest.newBuilder().setServiceId(environmentKey.toUpperCase()).setFrom(from.toString(formatter))
				.setTo(to.toString(formatter)).setViewId(view.id).setVolumeType(ValidationUtil.VolumeType.hits).setIncludeStacktrace(true);

		if (StringUtils.isNotEmpty(deploymentName)) builder.addDeployment(deploymentName);
		if (StringUtils.isNotEmpty(applicationName)) builder.addApp(applicationName);

		return builder.build();
	}

	private void logConfigData() {
		LOGGER.info("environmentKey :" + environmentKey);
		LOGGER.info("appHost :" + appHost);
		LOGGER.info("deploymentName :" + deploymentName);
		LOGGER.info("applicationName :" + applicationName);
		LOGGER.info("daysSpan :" + daysSpan);
		LOGGER.info("viewId :" + viewId);
	}

	private boolean validateVolumeResponse(UrlClient.Response<EventsResult> eventsResponse) {
		if (eventsResponse.data.events == null) {
			LOGGER.error("Failed getting OverOps events, please check Application Name or Deployment Name.");
			return false;
		} else if (eventsResponse.isBadResponse()) {
			LOGGER.error("Bad API Response " + appHost + " " + deploymentName + " " + applicationName);
			return false;
		}
		return true;
	}
}
