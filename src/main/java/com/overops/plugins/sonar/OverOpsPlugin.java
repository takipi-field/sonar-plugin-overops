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
import com.overops.plugins.sonar.measures.OverOpsQualityGateStat;
import com.overops.plugins.sonar.rules.RuleDefinitionImplementation;
import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient;
import com.takipi.common.util.TimeUtil;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.sonar.api.Plugin;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.overops.plugins.sonar.settings.OverOpsProperties.*;

public class OverOpsPlugin implements Plugin {
	private static final Logger LOGGER = Loggers.get(OverOpsPlugin.class);
	public static final long DEFAULT_SPAN_DAYS = 1L;
	public static final String DEFAULT_VIEWID = "All Exceptions";
	public static final String DEFAULT_OVER_OPS_API_HOST = "https://api.overops.com";
	public static final String SONAR_HOST_PROPERTY = "sonar.host.url";
	public static String SONAR_HOST_URL;
	public static String AUTH_DATA;

	public static String serviceId;
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
	public static String viewName;
	public static String newErrorGate;
	public static String resurfacedErrorGate;
	public static String criticalErrorTypes;
	public static String increasingErrorGate;
	public static String increasingErrorGateActiveTimeWindow;
	public static String increasingErrorGateBaselineTimeWindow;
	public static String increasingErrorGateErrorVolumeThreshold;
	public static String increasingErrorGateErrorRateThreshold;
	public static String increasingErrorGateErrorRegressionDelta;
	public static String increasingErrorGateErrorCriticalRegressionThreshold;
	public static String increasingErrorGateApplySeasonality;
	public static String login;
	public static String pass;

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
		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, serviceId, viewName);
		if (view == null) {
			LOGGER.error("Failed getting OverOps View, please check viewId or environmentKey.");
			return false;
		}

		UrlClient.Response<EventsResult> volumeResponse = getVolumeResponse(view);

		if (!validateVolumeResponse(volumeResponse)) {
			return false;
		}

		volumeResult = volumeResponse.data;

        overOpsEventsStatistic.setOverOpsQualityGateStat(new OverOpsQualityGateStat(getRelialabilityReport()));

        for (EventResult event : volumeResult.events) {
			LOGGER.info("");
			LOGGER.info("                      ~~~~~~~~                    ");
				LOGGER.info(event.id + " " +event.type +  "  " + event.error_location.prettified_name);
			LOGGER.info("                      ~~~~~~~~                    ");
			LOGGER.info("");
			overOpsEventsStatistic.add(event);
		}

		return true;
	}

    private ReliabilityReport getRelialabilityReport() {
        ReliabilityReportInput reportInput = new ReliabilityReportInput();
        reportInput.timeFilter = TimeUtil.getLastWindowTimeFilter(TimeUnit.MINUTES.toMillis(daysSpan));
        reportInput.environments = serviceId;
        reportInput.view = viewName;
        reportInput.outputDrillDownSeries = true;
        reportInput.applications = applicationName;
        reportInput.deployments = deploymentName;
        reportInput.mode = ReliabilityReportInput.DEFAULT_REPORT;

        return ReliabilityReport.execute(apiClient, reportInput);
    }


    private UrlClient.Response<EventsResult> getVolumeResponse(SummarizedView view) {
		EventsVolumeRequest eventsVolumeRequest = getVolumeRequest(view);
		LOGGER.info("");
		LOGGER.info("                      ~~~~~~~~                    ");
		LOGGER.info(eventsVolumeRequest.urlPath());
		try {
		    StringBuilder stringBuilder = new StringBuilder("?");
		    for (String p : eventsVolumeRequest.queryParams()) {
		        stringBuilder.append("&").append(p);
            }
			LOGGER.info(stringBuilder.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		LOGGER.info("                      ~~~~~~~~                    ");
		LOGGER.info("");
		return apiClient.get(eventsVolumeRequest);
	}

	private void getConfigProperties(Configuration config) {
		SONAR_HOST_URL = config.get(SONAR_HOST_PROPERTY).orElse("No host found");
		serviceId = config.get(OverOpsProperties.SONAR_OVEROPS_ENVIRONMENT_ID).orElse(null);
		appHost = config.get(OverOpsProperties.SONAR_OVEROPS_API_HOST).orElse(DEFAULT_OVER_OPS_API_HOST);
		apiKey = config.get(OverOpsProperties.SONAR_OVEROPS_API_KEY).orElse(null);
		deploymentName = config.get(OverOpsProperties.SONAR_OVEROPS_DEP_NAME).orElse(null);
		applicationName = config.get(OverOpsProperties.SONAR_OVEROPS_APP_NAME).orElse(null);
		daysSpan = config.getLong(OverOpsProperties.SONAR_OVEROPS_SPAN_DAYS).orElse(DEFAULT_SPAN_DAYS);
		viewName = config.get(OverOpsProperties.SONAR_OVEROPS_VIEW_ID).orElse(DEFAULT_VIEWID);
		newErrorGate = config.get(SONAR_OVEROPS_NEW_ERROR_GATE)
				.orElse(NEW_ERRORS_GATE_DEFAULT);
		resurfacedErrorGate = config.get(SONAR_OVEROPS_RESURFACED_ERROR_GATE)
				.orElse(RESURFACED_ERROR_GATE_DEFAULT);
		criticalErrorTypes = config.get(SONAR_OVEROPS_CRITICAL_ERROR_GATE)
				.orElse(CRITICAL_EXCEPTION_TYPES_DEFAULT);
		increasingErrorGate = config.get(SONAR_OVEROPS_INCREASING_ERROR_GATE)
				.orElse(INCREASING_ERROR_GATE_DEFAULT);
		increasingErrorGateActiveTimeWindow = config.get(SONAR_OVEROPS_INCREASING_ACTIVE_TIME_WINDOW)
				.orElse(ACTIVE_TIME_WINDOW_DEFAULT);
		increasingErrorGateBaselineTimeWindow = config.get(SONAR_OVEROPS_INCREASING_BASELINE_TIME_WINDOW)
				.orElse(BASELINE_TIME_WINDOW_DEFAULT);
		increasingErrorGateErrorVolumeThreshold = config.get(SONAR_OVEROPS_INCREASING_ERROR_VOLUME_THRESHOLD)
				.orElse(ERROR_VOLUME_THRESHOLD_DEFAULT);
		increasingErrorGateErrorRateThreshold = config.get(SONAR_OVEROPS_INCREASING_ERROR_RATE_THRESHOLD)
				.orElse(ERROR_RATE_THRESHOLD_DEFAULT);
		increasingErrorGateErrorRegressionDelta = config.get(SONAR_OVEROPS_INCREASING_REGRESSION_DELTA)
				.orElse(REGRESSION_DELTA_DEFAULT);
		increasingErrorGateErrorCriticalRegressionThreshold = config.get(SONAR_OVEROPS_INCREASING_CRITICAL_REGRESSION_THRESHOLD)
				.orElse(CRITICAL_REGRESSION_THRESHOLD_DEFAULT);
		increasingErrorGateApplySeasonality = config.get(SONAR_OVEROPS_INCREASING_APPLY_SEASONALITY)
				.orElse(APPLY_SEASONALITY_DEFAULT);

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
				StringUtils.isEmpty(serviceId) ||
				StringUtils.isEmpty(appHost) ||
				StringUtils.isEmpty(viewName)) {
			LOGGER.error("Failed to process OverOps sensor one of [apiKey, environmentKey, appHost, viewId] properties is empty. Please check project OverOps configuration.");
			return false;
		}

		return true;
	}

	private EventsVolumeRequest getVolumeRequest(SummarizedView view) {
		EventsVolumeRequest.Builder builder = EventsVolumeRequest.newBuilder().setServiceId(serviceId.toUpperCase()).setFrom(from.toString(formatter))
				.setTo(to.toString(formatter)).setViewId(view.id).setVolumeType(ValidationUtil.VolumeType.hits).setIncludeStacktrace(true);

		if (StringUtils.isNotEmpty(deploymentName)) builder.addDeployment(deploymentName);
		if (StringUtils.isNotEmpty(applicationName)) builder.addApp(applicationName);

		return builder.build();
	}

	private void logConfigData() {
		LOGGER.info("environmentKey :" + serviceId);
		LOGGER.info("appHost :" + appHost);
		LOGGER.info("deploymentName :" + deploymentName);
		LOGGER.info("applicationName :" + applicationName);
		LOGGER.info("daysSpan :" + daysSpan);
		LOGGER.info("viewId :" + viewName);
		LOGGER.info("newErrorGate :" + newErrorGate);
		LOGGER.info("resurfacedErrorGate :" + resurfacedErrorGate);
		LOGGER.info("criticalErrorTypes :" + criticalErrorTypes);
		LOGGER.info("increasingErrorGate :" + increasingErrorGate);
		LOGGER.info("increasingErrorGateActiveTimeWindow :" + increasingErrorGateActiveTimeWindow);
		LOGGER.info("increasingErrorGateBaselineTimeWindow :" + increasingErrorGateBaselineTimeWindow);
		LOGGER.info("increasingErrorGateErrorVolumeThreshold :" + increasingErrorGateErrorVolumeThreshold);
		LOGGER.info("increasingErrorGateErrorRateThreshold :" + increasingErrorGateErrorRateThreshold);
		LOGGER.info("increasingErrorGateErrorRegressionDelta :" + increasingErrorGateErrorRegressionDelta);
		LOGGER.info("increasingErrorGateErrorCriticalRegressionThreshold :" + increasingErrorGateErrorCriticalRegressionThreshold);
		LOGGER.info("increasingErrorGateApplySeasonality :" + increasingErrorGateApplySeasonality);

		LOGGER.info("login :" + login);
		LOGGER.info("pass :" + pass);
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
