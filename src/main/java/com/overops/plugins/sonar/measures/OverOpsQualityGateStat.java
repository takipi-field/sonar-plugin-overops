package com.overops.plugins.sonar.measures;

import com.takipi.api.client.functions.output.EventRow;
import com.takipi.api.client.functions.output.RegressionRow;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.functions.output.ReliabilityReportRow;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OverOpsQualityGateStat {
    public Set<String> newEventsIds = new HashSet<>();
    public Set<String> resurfacedEventsIds = new HashSet<>();
    public Set<String> criticalEventsIds = new HashSet<>();
    public Set<String> increasingEventsIds = new HashSet<>();

    public OverOpsQualityGateStat(ReliabilityReport reliabilityReport) {
        if (reliabilityReport == null || reliabilityReport.items == null) {
            return;
        }

        for (Map.Entry<ReliabilityReportRow.Header, ReliabilityReport.ReliabilityReportItem> entry : reliabilityReport.items.entrySet()) {
            ReliabilityReport.ReliabilityReportItem rrItem = entry.getValue();
            addNewErrors(rrItem);
            addIncreasingErrors(rrItem);
            addCriticalErrors(rrItem);
            addResurfacedErrors(rrItem);
        }
    }

    private void addResurfacedErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        for (EventRow row : rrItem.errors) {
            if((row.labels != null) &&
                    (row.labels.indexOf("Resurfaced") != -1)) {
                resurfacedEventsIds.add(row.id);
            }
        }
    }

    private void addCriticalErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        for (EventRow row : rrItem.failures) {
            criticalEventsIds.add(row.id );
        }
    }

    private void addIncreasingErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        for (RegressionRow row : rrItem.geIncErrors(true, true)) {
            increasingEventsIds.add(row.id);
        }
    }

    private void addNewErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        for (RegressionRow row : rrItem.getNewErrors(true, true)) {
            newEventsIds.add(row.id);
        }
    }
}
