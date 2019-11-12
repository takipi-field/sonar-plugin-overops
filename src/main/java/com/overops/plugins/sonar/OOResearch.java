package com.overops.plugins.sonar;

import com.takipi.api.client.ApiClient;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.functions.input.ReliabilityReportInput;
import com.takipi.api.client.functions.output.RegressionRow;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.functions.output.ReliabilityReportRow;
import com.takipi.api.core.url.UrlClient;
import com.takipi.common.util.TimeUtil;

import java.net.HttpURLConnection;
import java.util.Map;
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
        int time_span = 24*60*3;

        ReliabilityReportInput reportInput = new ReliabilityReportInput();
        reportInput.timeFilter = TimeUtil.getLastWindowTimeFilter(TimeUnit.MINUTES.toMillis(time_span));
        reportInput.environments = serviceId;
        reportInput.view = view;
        reportInput.outputDrillDownSeries = true;
        reportInput.applications = application;
        reportInput.deployments = deployment;
        reportInput.mode = ReliabilityReportInput.DEFAULT_REPORT;

        ReliabilityReport reliabilityReport = ReliabilityReport.execute(apiClient, reportInput);

        for (Map.Entry<ReliabilityReportRow.Header, ReliabilityReport.ReliabilityReportItem> entry : reliabilityReport.items.entrySet()) {
            ReliabilityReportRow.Header rrHeader = entry.getKey();
            ReliabilityReport.ReliabilityReportItem rrItem = entry.getValue();

            System.out.println("  ");
            System.out.println(" ------------------------------------- ");
            System.out.println(" New errors ");
            System.out.println("  ");
            for (RegressionRow row : rrItem.getNewErrors(true, true)) {
                System.out.println("  " + row.id + " in " + row.error_location);
            }
            System.out.println(" ------------------------------------- ");
            System.out.println(" ------------------------------------- ");
            System.out.println(" Increasing errors ");
            System.out.println("  ");
            for (RegressionRow row : rrItem.geIncErrors(true, true)) {
                System.out.println("  " + row.id + " in " + row.error_location);
            }
            System.out.println(" ------------------------------------- ");
            System.out.println("  ");
        }
    }
}
