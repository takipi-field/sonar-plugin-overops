package com.overops.plugins.sonar;

import com.takipi.api.client.ApiClient;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.functions.input.EventFilterInput;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.EventRow;
import com.takipi.api.client.functions.output.RegressionRow;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.functions.output.ReliabilityReportRow;
import com.takipi.api.client.request.event.EventsRequest;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.regression.RegressionInput;
import com.takipi.api.client.util.regression.RegressionUtil;
import com.takipi.api.client.util.settings.RegressionSettings;
import com.takipi.api.client.util.validation.ValidationUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient;
import com.takipi.common.util.Pair;
import com.takipi.common.util.TimeUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.overops.plugins.sonar.OverOpsPlugin.apiClient;

public class OOResearch {
    public static void main(String[] args) {
        ApiClient apiClient = RemoteApiClient.newBuilder()
                .setHostname("https://api.overops.com")
                .setApiKey("0ZRwKT9HEiISRXYJpttmXv21G8BeDAbeiCPisEAh")
                .setDefaultLogLevel(UrlClient.LogLevel.WARN)
                .setResponseLogLevel(HttpURLConnection.HTTP_CONFLICT, UrlClient.LogLevel.INFO).build();

        String serviceId = "S39410";
        String application = "App1";
        String view = "View1";
        String deployment = "Dep1";
        int daysSpan = 7;
        int time_span = 24 * 60 * daysSpan;
        DateTime to = DateTime.now();
        DateTime from = to.minusDays((int) daysSpan);
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withZoneUTC();

        ReliabilityReportInput reportInput = new ReliabilityReportInput();
        reportInput.timeFilter = TimeUtil.getLastWindowTimeFilter(TimeUnit.MINUTES.toMillis(time_span));
        reportInput.environments = serviceId;
        reportInput.view = view;
        reportInput.outputDrillDownSeries = true;
        reportInput.applications = application;
        reportInput.deployments = deployment;
        reportInput.mode = ReliabilityReportInput.DEFAULT_REPORT;
        //reportInput.regressionInput = getRegressionInput();

        SummarizedView summarizedView = ViewUtil.getServiceViewByName(apiClient, serviceId, view);

        EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(serviceId).setFrom(from.toString(formatter))
                .setTo(to.toString(formatter)).setViewId(summarizedView.id)
                .addApp(application)
                .addDeployment(deployment)
                .setVolumeType(ValidationUtil.VolumeType.all)
                .setIncludeStacktrace(true)
                .build();

        EventsRequest allEventsRequest = EventsRequest.newBuilder().setServiceId(serviceId).setFrom(from.toString(formatter))
                .setTo(to.toString(formatter)).setViewId(summarizedView.id)
                .addApp(application)
                .addDeployment(deployment)
                .setIncludeStacktrace(true)
                .build();
        UrlClient.Response<EventsResult> alleEventsResult = apiClient.get(allEventsRequest);



        ReliabilityReport reliabilityReport = ReliabilityReport.execute(apiClient, reportInput);

        Set<String> eventIdSetFromVolume = new HashSet<>();
        Set<String> eventIdSetFromReport = new HashSet<>();

        for (Map.Entry<ReliabilityReportRow.Header, ReliabilityReport.ReliabilityReportItem> entry : reliabilityReport.items.entrySet()) {
            ReliabilityReportRow.Header rrHeader = entry.getKey();
            ReliabilityReport.ReliabilityReportItem rrItem = entry.getValue();

            printNewErrors(eventIdSetFromReport, rrItem);
            printIncreasingErrors(rrItem);
            printCriticalErrors(rrItem);
            printResurfacedErrors(rrItem);

            System.out.println(" ------------------------------------- ");
            System.out.println("  ");
        }
        UrlClient.Response<EventsResult> eventsResultResponse = apiClient.get(eventsVolumeRequest);

        System.out.println(" ------------------------------------- ");
        System.out.println(" ------------------------------------- ");
        System.out.println(" Volume events");
        System.out.println("  ");
        if (eventsResultResponse.data != null && eventsResultResponse.data.events != null) {
            eventsResultResponse.data.events.forEach( event -> {
                System.out.println("  " + event.id + " in " + event.error_location + "  labels " + event.labels);
                eventIdSetFromVolume.add(event.id);
            });
        }

        Set<String> intersection = new HashSet<>();
        eventIdSetFromReport.stream().forEach(id-> {
            if(eventIdSetFromVolume.contains(id)){
                intersection.add(id);
            }
        });
        eventIdSetFromReport.removeAll(eventIdSetFromVolume);
//        System.out.println(" ------------------------------------- ");
//        System.out.println(" ------------------------------------- ");
//        System.out.println(" Report event ids minus volume, should be empty ");
//        System.out.println("  ");
//
//        eventIdSetFromReport.forEach(id -> {
//            System.out.print("{ id:" + id + " } , ");
//        });
    }

    private static void printResurfacedErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        System.out.println("  ");
        System.out.println(" ------------------------------------- ");
        System.out.println(" Resurfaced events ");
        System.out.println("  ");
        for (EventRow row : rrItem.errors) {
            if((row.labels != null) &&
                    (row.labels.indexOf("Resurfaced") != -1))
            System.out.print("{ id:" + row.id + " , error_location: " + row.error_location + " } , ");
        }
        System.out.println("  ");
    }

    private static void printCriticalErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        System.out.println("  ");
        System.out.println(" ------------------------------------- ");
        System.out.println(" Critical events ");
        System.out.println("  ");
        for (EventRow row : rrItem.failures) {
            System.out.print("{ id:" + row.id + " , error_location: " + row.error_location + " } , ");
        }
        System.out.println("  ");
    }

    private static void printIncreasingErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        System.out.println(" ------------------------------------- ");
        System.out.println(" Increasing events ");
        System.out.println("  ");
        for (RegressionRow row : rrItem.geIncErrors(true, true)) {
            System.out.print("{ id:" + row.id + " , error_location: " + row.error_location + " } , ");
        }
        System.out.println("  ");
    }

    private static void printNewErrors(Set<String> eventIdSetFromReport, ReliabilityReport.ReliabilityReportItem rrItem) {
        System.out.println("  ");
        System.out.println(" ------------------------------------- ");
        System.out.println(" New events ");
        System.out.println("  ");
        int i = 1;
        for (RegressionRow row : rrItem.getNewErrors(true, true)) {

            System.out.print("{ id:" + row.id + " , error_location: " + row.error_location + " } , ");
            eventIdSetFromReport.add(row.id);
            if (i % 4 == 0) {
                System.out.println("  ");
            }
            i++;
        }
        System.out.println("  ");
    }

    private static RegressionInput getRegressionInput() {
        RegressionInput regressionInput = new RegressionInput();
        List<String> deployments = Arrays.asList(OverOpsPlugin.deploymentName);
        Pair<DateTime, DateTime> deploymentsActiveWindow =
                RegressionUtil.getDeploymentsActiveWindow(apiClient, OverOpsPlugin.serviceId, deployments);

        regressionInput.serviceId = OverOpsPlugin.serviceId;
        regressionInput.viewId = OverOpsPlugin.viewName;
        regressionInput.applictations = Arrays.asList(OverOpsPlugin.applicationName);
        regressionInput.deployments = deployments;
        regressionInput.activeWindowStart = deploymentsActiveWindow.getFirst();
        regressionInput.applySeasonality = "true".equalsIgnoreCase(OverOpsPlugin.increasingErrorGateApplySeasonality);
        regressionInput.criticalExceptionTypes = Arrays.asList(OverOpsPlugin.criticalErrorTypes.split(","));
        regressionInput.activeTimespan = (int)TimeUnit.MILLISECONDS
                .toMinutes(deploymentsActiveWindow.getSecond().getMillis() - deploymentsActiveWindow.getFirst().getMillis());

        //TODO do we need fill if yes then how? regressionInput.baselineTimespan;
        //TODO do we need fill if yes then how? regressionInput.baselineTime;
        //TODO from previous  how to get it
        //regressionInput.minVolumeThreshold;
        //TODO from previous  how to get it
        //regressionInput.minErrorRateThreshold;
        //TODO do we need fill if yes then how? regressionInput.regressionDelta;
        //TODO do we need fill if yes then how? regressionInput.criticalRegressionDelta;

        //TODO do we need fill if yes then how? regressionInput.typeThresholdsMap;

        //TODO do we need fill? regressionInput.servers
        //TODO do we need fill? regressionInput.events;
        //TODO do we need fill? regressionInput.baselineGraph;
        return regressionInput;
    }

//    public Pair<RegressionInput, RegressionUtil.RegressionWindow> getRegressionInput(String serviceId, String viewId,
//                                                                                     EventFilterInput input, RegressionUtil.RegressionWindow window,
//                                                                                     Pair<DateTime, DateTime> timeSpan, boolean newOnly) {
//
//        RegressionSettings regressionSettings;// = getRegressionSettings(serviceId);
//
//        RegressionInput regressionInput = new RegressionInput();
//
//        RegressionInput existingInput;
//
////        if (input instanceof RegressionsInput) {
////            existingInput = ((RegressionsInput)input).regressionInput;
////        } else {
////            existingInput = null;
////        }
//
//        if (existingInput != null) {
//
//            regressionInput.criticalExceptionTypes = existingInput.criticalExceptionTypes;
//            regressionInput.minVolumeThreshold = existingInput.minVolumeThreshold;
//            regressionInput.minErrorRateThreshold = existingInput.minErrorRateThreshold;
//            regressionInput.typeThresholdsMap = existingInput.typeThresholdsMap;
//
//            if (!newOnly) {
//                regressionInput.regressionDelta = existingInput.regressionDelta;
//                regressionInput.criticalRegressionDelta = existingInput.criticalRegressionDelta;
//                regressionInput.applySeasonality = existingInput.applySeasonality;
//            }
//        } else {
//
//            Collection<String> criticalExceptionTypes = regressionSettings.getCriticalExceptionTypes();
//
//            regressionInput.criticalExceptionTypes = criticalExceptionTypes;
//            regressionInput.minVolumeThreshold = regressionSettings.error_min_volume_threshold;
//            regressionInput.minErrorRateThreshold = regressionSettings.error_min_rate_threshold;
//
//            if (!newOnly) {
//                regressionInput.regressionDelta = regressionSettings.error_regression_delta;
//                regressionInput.criticalRegressionDelta = regressionSettings.error_critical_regression_delta;
//                regressionInput.applySeasonality = regressionSettings.apply_seasonality;
//            }
//        }
//
//        regressionInput.activeTimespan = (int)TimeUnit.MILLISECONDS
//                .toMinutes(timeSpan.getSecond().getMillis() - timeSpan.getFirst().getMillis());
//
//        if ((CollectionUtil.safeIsEmpty(regressionInput.deployments))) {
//            regressionInput.activeWindowStart = timeSpan.getFirst();
//        }
//
//        regressionInput.baselineTimespan = regressionSettings.min_baseline_timespan;
//
//        RegressionWindow regressionWindow;
//        if (window == null) {
//            regressionWindow = ApiCache.getRegressionWindow(apiClient, regressionInput);
//        } else {
//            regressionWindow = window;
//        }
//
//        if ((!CollectionUtil.safeIsEmpty(regressionInput.deployments))
//                && (!regressionWindow.deploymentFound)) {
//            return null;
//        }
//
//        int expandedBaselineTimespan = expandBaselineTimespan(regressionSettings.baseline_timespan_factor,
//                regressionSettings.min_baseline_timespan,
//                regressionWindow);
//
//        regressionInput.activeWindowStart = regressionWindow.activeWindowStart;
//        regressionInput.activeTimespan = regressionWindow.activeTimespan;
//        regressionInput.baselineTimespan = expandedBaselineTimespan;
//
//        regressionInput.applictations = getApplications(serviceId, input, true, false);
//        regressionInput.servers = getServers(serviceId, input);
//
//        return Pair.of(regressionInput, regressionWindow);
//
//    }
}
