package com.overops.plugins.sonar.measures;

import com.takipi.api.client.functions.output.EventRow;
import com.takipi.api.client.functions.output.RegressionRow;
import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.functions.output.ReliabilityReportRow;

import java.util.*;

public class OverOpsQualityGateStat {
    public static final String NEW_QG_MARKER = "New";
    public static final String CRITICAL_QG_MARKER = "Critical";
    public static final String RESURFACED_QG_MARKER = "Resurfaced";
    public static final String INCREASING_QG_MARKER = "Increasing";

    public Map<String, RegressionRow> newEventsIds = new HashMap();
    public Map<String, EventRow> resurfacedEventsIds = new HashMap<>();
    public Map<String, EventRow> criticalEventsIds = new HashMap<>();
    public Map<String, RegressionRow> increasingEventsIds = new HashMap<>();

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
                resurfacedEventsIds.put(row.id, row);
            }
        }
        for (EventRow row : rrItem.failures) {
            if((row.labels != null) &&
                    (row.labels.indexOf("Resurfaced") != -1)) {
                resurfacedEventsIds.put(row.id, row);
            }
        }
    }

    public static String getKey(Set<String> qualityGates) {
        return String.join(".", qualityGates);
    }

    private void addCriticalErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        for (EventRow row : rrItem.failures) {
            criticalEventsIds.put(row.id, row);
        }
    }

    private void addIncreasingErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        for (RegressionRow row : rrItem.geIncErrors(true, true)) {
            increasingEventsIds.put(row.id, row);
        }
    }

    private void addNewErrors(ReliabilityReport.ReliabilityReportItem rrItem) {
        for (RegressionRow row : rrItem.getNewErrors(true, true)) {
            newEventsIds.put(row.id, row);
        }
    }

    public Set<String> getQualityGates(String  id) {
        Set<String> result = new TreeSet<>();
        if (newEventsIds.keySet().contains(id)) {
            result.add(NEW_QG_MARKER);
        }
        if (resurfacedEventsIds.keySet().contains(id)) {
            result.add(RESURFACED_QG_MARKER);
        }
        if (criticalEventsIds.keySet().contains(id)) {
            result.add(CRITICAL_QG_MARKER);
        }
        if (increasingEventsIds.keySet().contains(id)) {
            result.add(INCREASING_QG_MARKER);
        }

        if (result.size()==0) {
            result.add("NO_QUALITY_GATE_IT_SHOULD_BE_VERIFIED");
        }

        return result;
    }
}
