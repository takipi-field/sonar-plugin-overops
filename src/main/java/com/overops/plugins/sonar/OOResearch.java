package com.overops.plugins.sonar;

import com.takipi.api.client.ApiClient;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.RegressionRow;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.functions.output.ReliabilityReportRow;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient;
import com.takipi.common.util.TimeUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
        int daysSpan = 2;
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

        SummarizedView summarizedView = ViewUtil.getServiceViewByName(apiClient, serviceId, view);

        EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(serviceId).setFrom(from.toString(formatter))
                .setTo(to.toString(formatter)).setViewId(summarizedView.id)
                .addApp(application)
                .addDeployment(deployment)
                .setVolumeType(ValidationUtil.VolumeType.hits)
                .setIncludeStacktrace(true)
                .build();

        ReliabilityReport reliabilityReport = ReliabilityReport.execute(apiClient, reportInput);

        Set<String> eventIdSetFromVolume = new HashSet<>();
        Set<String> eventIdSetFromReport = new HashSet<>();

        for (Map.Entry<ReliabilityReportRow.Header, ReliabilityReport.ReliabilityReportItem> entry : reliabilityReport.items.entrySet()) {
            ReliabilityReportRow.Header rrHeader = entry.getKey();
            ReliabilityReport.ReliabilityReportItem rrItem = entry.getValue();

            System.out.println("  ");
            System.out.println(" ------------------------------------- ");
            System.out.println(" New events ");
            System.out.println("  ");
            for (RegressionRow row : rrItem.getNewErrors(true, true)) {
                System.out.println("  " + row.id + " in " + row.error_location);
                eventIdSetFromReport.add(row.id);
            }
            System.out.println(" ------------------------------------- ");
            System.out.println(" ------------------------------------- ");
            System.out.println(" Increasing events ");
            System.out.println("  ");
            for (RegressionRow row : rrItem.geIncErrors(true, true)) {
                System.out.println("  " + row.id + " in " + row.error_location);
            }
            System.out.println(" ------------------------------------- ");
            System.out.println("  ");
        }
        UrlClient.Response<EventsResult> eventsResultResponse = apiClient.get(eventsVolumeRequest);

        System.out.println(" ------------------------------------- ");
        System.out.println(" ------------------------------------- ");
        System.out.println(" Volume events");
        System.out.println("  ");
        if (eventsResultResponse.data != null) {
            eventsResultResponse.data.events.forEach( event -> {
                System.out.println("  " + event.id + " in " + event.error_location);
                eventIdSetFromVolume.add(event.id);
            });
        }

        eventIdSetFromReport.removeAll(eventIdSetFromVolume);
        System.out.println(" ------------------------------------- ");
        System.out.println(" ------------------------------------- ");
        System.out.println(" Report event ids minus volume, should be empty ");
        System.out.println("  ");
        eventIdSetFromReport.forEach(id -> {
            System.out.println(" id " + id);
        });
    }
}
