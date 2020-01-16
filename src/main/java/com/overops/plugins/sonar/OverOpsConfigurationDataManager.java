package com.overops.plugins.sonar;

import com.overops.plugins.sonar.measures.OverOpsEventsStatistic;
import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.common.util.TimeUtil;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.overops.plugins.sonar.settings.OverOpsProperties.*;

public class OverOpsConfigurationDataManager {
    private static final Logger log = Loggers.get(OverOpsConfigurationDataManager.class);

    public static String SONAR_HOST_URL;
    public static String AUTH_DATA;
    public static String serviceId;
    public static String apiHost;
    public static String appHost;
    public static String apiKey;
    public static String deploymentName;
    public static String applicationName;
    public static DateTime to;
    public static DateTime from;
    public static DateTimeFormatter formatter;
    public static RemoteApiClient apiClient;
    public static long daysSpan;
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
    public static OverOpsEventsStatistic overOpsEventsStatistic;

    public static boolean getOverOpsDataAndCreateStatistic(Configuration config) {
        if (overOpsEventsStatistic != null) {
            return true;
        }

        overOpsEventsStatistic = new OverOpsEventsStatistic();

        getConfigProperties(config);

        if (!validateConfigData()) {
            return false;
        }

        apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(apiKey).setHostname(apiHost).build();
        overOpsEventsStatistic.setOverOpsQualityGateStat(getReliabilityReport());

        writeToFileOverOpsEventsStatistic(overOpsEventsStatistic);

        return true;
    }

    public static void logConfigData() {
        log.info("environmentKey :" + serviceId);
        log.info("apiHost :" + apiHost);
        log.info("appHost :" + appHost);
        log.info("deploymentName :" + deploymentName);
        log.info("applicationName :" + applicationName);
        log.info("daysSpan :" + daysSpan);
        log.info("viewId :" + viewName);
        log.info("newErrorGate :" + newErrorGate);
        log.info("resurfacedErrorGate :" + resurfacedErrorGate);
        log.info("criticalErrorTypes :" + criticalErrorTypes);
        log.info("increasingErrorGate :" + increasingErrorGate);
        log.info("increasingErrorGateActiveTimeWindow :" + increasingErrorGateActiveTimeWindow);
        log.info("increasingErrorGateBaselineTimeWindow :" + increasingErrorGateBaselineTimeWindow);
        log.info("increasingErrorGateErrorVolumeThreshold :" + increasingErrorGateErrorVolumeThreshold);
        log.info("increasingErrorGateErrorRateThreshold :" + increasingErrorGateErrorRateThreshold);
        log.info("increasingErrorGateErrorRegressionDelta :" + increasingErrorGateErrorRegressionDelta);
        log.info("increasingErrorGateErrorCriticalRegressionThreshold :" + increasingErrorGateErrorCriticalRegressionThreshold);
        log.info("increasingErrorGateApplySeasonality :" + increasingErrorGateApplySeasonality);
    }

    private static void writeToFileOverOpsEventsStatistic(OverOpsEventsStatistic overOpsEventsStatistic) {
        try {
            FileOutputStream file = new FileOutputStream("too.txt");
            ObjectOutputStream out = new ObjectOutputStream(file);

            out.writeObject(overOpsEventsStatistic);

            out.close();
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ReliabilityReport getReliabilityReport() {
        ReliabilityReportInput reportInput = new ReliabilityReportInput();
        reportInput.timeFilter = TimeUtil.getLastWindowTimeFilter(TimeUnit.DAYS.toMillis(daysSpan));
        reportInput.environments = serviceId;
        reportInput.view = viewName;
        reportInput.outputDrillDownSeries = true;
        reportInput.applications = applicationName;
        reportInput.deployments = deploymentName;
        reportInput.mode = ReliabilityReportInput.DEFAULT_REPORT;
        reportInput.requestStackframes = true;

        ReliabilityReport reliabilityReport = ReliabilityReport.execute(apiClient, reportInput);
        return reliabilityReport;
    }

    private static void getConfigProperties(Configuration config) {
        SONAR_HOST_URL = config.get(SONAR_HOST_PROPERTY).orElse("No host found");
        serviceId = config.get(OverOpsProperties.SONAR_OVEROPS_ENVIRONMENT_ID).orElse(null);
        apiHost = config.get(OverOpsProperties.SONAR_OVEROPS_API_HOST).orElse(OVER_OPS_API_HOST_DEFAULT);
        apiKey = config.get(OverOpsProperties.SONAR_OVEROPS_API_KEY).orElse(null);
        appHost = config.get(OverOpsProperties.SONAR_OVEROPS_APP_HOST).orElse(null);
        deploymentName = config.get(OverOpsProperties.SONAR_OVEROPS_DEP_NAME).orElse(null);
        applicationName = config.get(OverOpsProperties.SONAR_OVEROPS_APP_NAME).orElse(null);
        daysSpan = config.getLong(OverOpsProperties.SONAR_OVEROPS_SPAN_DAYS).orElse(SPAN_DAYS_DEFAULT);
        viewName = config.get(OverOpsProperties.SONAR_OVEROPS_VIEW_ID).orElse(VIEW_ID_DEFAULT);
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

    private static boolean validateConfigData() {
        if (StringUtils.isEmpty(apiKey) ||
                StringUtils.isEmpty(serviceId) ||
                StringUtils.isEmpty(apiHost) ||
                StringUtils.isEmpty(viewName)) {
            log.error("Failed to process OverOps sensor one of [apiKey, environmentKey, appHost, viewId] properties is empty. Please check project OverOps configuration.");
            return false;
        }

        return true;
    }
}
