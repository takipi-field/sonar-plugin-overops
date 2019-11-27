package com.overops.plugins.sonar;

import com.overops.plugins.sonar.measures.OverOpsEventsStatistic;
import com.overops.plugins.sonar.measures.MeasureDefinition;
import com.overops.plugins.sonar.measures.OverOpsMetrics;
import com.overops.plugins.sonar.measures.OverOpsQualityGateStat;
import com.overops.plugins.sonar.rules.RuleDefinitionImplementation;
import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.RegressionRow;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.functions.output.Series;
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
import java.util.ArrayList;
import java.util.Base64;
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
	public static String adminUserName;
	public static String adminUserPassword;

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
		OverOpsQualityGateStat overOpsQualityGateStat = new OverOpsQualityGateStat(getReliabilityReport());
		addAdditionalEvents(volumeResult, overOpsQualityGateStat);
		overOpsEventsStatistic.setOverOpsQualityGateStat(overOpsQualityGateStat);

        for (EventResult event : volumeResult.events) {
			LOGGER.info("================== event id" + event.id + " type:" + event.type  + " ================================");
			LOGGER.info("");
			overOpsEventsStatistic.add(event);
			LOGGER.info("");
			LOGGER.info("==============================================================");
		}

		return true;
	}

	private void addAdditionalEvents(EventsResult volumeResult, OverOpsQualityGateStat overOpsQualityGateStat) {
		EventResult original = volumeResult.events.get(0);
		addIncreasing(volumeResult, overOpsQualityGateStat, (EventResult) original.clone());
		addResurfaced(volumeResult, overOpsQualityGateStat, (EventResult) original.clone());
		addCritical(volumeResult, overOpsQualityGateStat, (EventResult) original.clone());
		addNewAndCritical(volumeResult, overOpsQualityGateStat, (EventResult) original.clone());
		addNewAndResurfaced(volumeResult, overOpsQualityGateStat, (EventResult) original.clone());
		addIncreasingAndCritical(volumeResult, overOpsQualityGateStat, (EventResult) original.clone());
		addResurfacedAndCritical(volumeResult, overOpsQualityGateStat, (EventResult) original.clone());
	}

	private void addResurfacedAndCritical(EventsResult volumeResult, OverOpsQualityGateStat overOpsQualityGateStat, EventResult clone) {
		clone.id = String.valueOf(idRef++);
		decorateResurfaced(clone, overOpsQualityGateStat);
		decorateCritical(clone, overOpsQualityGateStat);
		clone.error_location.prettified_name = " [Resurfaced and Critical gate exception on some method]";
		clone.error_location.original_line_number += 6;
		clone.error_location.method_position += 6;
		volumeResult.events.add(clone);
	}

	private void addIncreasingAndCritical(EventsResult volumeResult, OverOpsQualityGateStat overOpsQualityGateStat, EventResult clone) {
		clone.id = String.valueOf(idRef++);
		decorateIncreasing(clone, overOpsQualityGateStat);
		decorateCritical(clone, overOpsQualityGateStat);
		clone.error_location.prettified_name = " [Increasing and Critical gate exception on some method]";
		clone.error_location.original_line_number += 6;
		clone.error_location.method_position += 6;
		volumeResult.events.add(clone);
	}

	private void addNewAndResurfaced(EventsResult volumeResult, OverOpsQualityGateStat overOpsQualityGateStat, EventResult clone) {
		clone.id = String.valueOf(idRef++);
		decorateNew(clone, overOpsQualityGateStat);
		decorateResurfaced(clone, overOpsQualityGateStat);
		clone.error_location.prettified_name = " [Resurfaced and Critical gate exception on some method]";
		clone.error_location.original_line_number += 5;
		clone.error_location.method_position += 5;
		volumeResult.events.add(clone);
	}

	private void addNewAndCritical(EventsResult volumeResult, OverOpsQualityGateStat overOpsQualityGateStat, EventResult clone) {
		clone.id = String.valueOf(idRef++);
		decorateNew(clone, overOpsQualityGateStat);
		decorateCritical(clone, overOpsQualityGateStat);
		clone.error_location.prettified_name = " [New and Critical gate exception on some method]";
		clone.error_location.original_line_number += 4;
		clone.error_location.method_position += 4;
		volumeResult.events.add(clone);
	}

	private void addCritical(EventsResult volumeResult, OverOpsQualityGateStat overOpsQualityGateStat, EventResult clone) {
		clone.id = String.valueOf(idRef++);
		decorateCritical(clone, overOpsQualityGateStat);
		volumeResult.events.add(clone);
	}

	private void addResurfaced(EventsResult volumeResult, OverOpsQualityGateStat overOpsQualityGateStat, EventResult clone) {
		clone.id = String.valueOf(idRef++);
		decorateResurfaced(clone, overOpsQualityGateStat);
		volumeResult.events.add(clone);
	}

	private void addIncreasing(EventsResult volumeResult, OverOpsQualityGateStat overOpsQualityGateStat, EventResult clone) {
		clone.id = String.valueOf(idRef++);
		decorateIncreasing(clone, overOpsQualityGateStat);
		volumeResult.events.add(clone);
	}

	public static int idRef = 10000;

	private void decorateNew(EventResult clone, OverOpsQualityGateStat overOpsQualityGateStat) {
		clone.error_location.prettified_name = " [New gate exception on some method]";
		overOpsQualityGateStat.newEventsIds.put(clone.id, null);
	}

	private void decorateIncreasing(EventResult clone, OverOpsQualityGateStat overOpsQualityGateStat) {
		clone.error_location.prettified_name = " [Increasing gate exception on some method]";
		Series<RegressionRow> series = new Series<>();
		series.columns = new ArrayList<>();
		RegressionRow regressionRow = new RegressionRow(series, 1);
		regressionRow.reg_delta = 1.569d;

		overOpsQualityGateStat.increasingEventsIds.put(clone.id, regressionRow);
	}

	private void decorateResurfaced(EventResult clone, OverOpsQualityGateStat overOpsQualityGateStat) {
		clone.error_location.prettified_name = " [Resurfaced gate exception on some method]";
		overOpsQualityGateStat.resurfacedEventsIds.put(clone.id, null);
	}

	private void decorateCritical(EventResult clone, OverOpsQualityGateStat overOpsQualityGateStat) {
		clone.error_location.prettified_name = " [Critical gate exception on some method]";
		overOpsQualityGateStat.criticalEventsIds.put(clone.id, null);
	}

	private ReliabilityReport getReliabilityReport() {
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
//		LOGGER.info("-------------------------------------------");
//		LOGGER.info("                                          ");
//		LOGGER.info(eventsVolumeRequest.urlPath());
		try {
		    StringBuilder stringBuilder = new StringBuilder("?");
		    for (String p : eventsVolumeRequest.queryParams()) {
		        stringBuilder.append("&").append(p);
            }
//			LOGGER.info(stringBuilder.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
//		LOGGER.info("                                          ");
//		LOGGER.info("-------------------------------------------");
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

		adminUserName = config.get(OverOpsProperties.SONAR_OVEROPS_USER_NAME).orElse("admin");
		adminUserPassword = config.get(OverOpsProperties.SONAR_OVEROPS_USER_PASSWORD).orElse("admin");
		AUTH_DATA = Base64.getEncoder().encodeToString(new StringBuilder()
				.append(adminUserName).append(":").append(adminUserPassword)
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
