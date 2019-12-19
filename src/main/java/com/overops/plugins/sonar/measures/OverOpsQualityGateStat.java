package com.overops.plugins.sonar.measures;

import com.takipi.api.client.functions.output.*;
import com.takipi.api.client.functions.output.ReliabilityReport.ReliabilityReportItem;

import java.util.*;

public class OverOpsQualityGateStat {

    public static final String NEW_QG_MARKER = "New";
    public static final String CRITICAL_QG_MARKER = "Critical";
    public static final String RESURFACED_QG_MARKER = "Resurfaced";
    public static final String INCREASING_QG_MARKER = "Increasing";

    public Map<String, RegressionRow> newEventsIds = new HashMap<>();
    public Map<String, EventRow> resurfacedEventsIds = new HashMap<>();
    public Map<String, EventRow> criticalEventsIds = new HashMap<>();
    public Map<String, RegressionRow> increasingEventsIds = new HashMap<>();

    public OverOpsQualityGateStat(ReliabilityReport reliabilityReport) {
        if (reliabilityReport == null || reliabilityReport.items == null) {
            System.out.println(" ReliabilityReport is empty");
            return;
        }

        ReliabilityReportItem rrItem = reliabilityReport.items.values().iterator().next();
        addNewErrors(rrItem);
        addIncreasingErrors(rrItem);
        addCriticalErrors(rrItem);
        addResurfacedErrors(rrItem);
    }

    private void addResurfacedErrors(ReliabilityReportItem rrItem) {
        for (EventRow row : rrItem.errors) {
            if ((row.labels != null) &&
                    (row.labels.indexOf("Resurfaced") != -1)) {
                resurfacedEventsIds.put(row.id, row);
            }
        }

        for (EventRow row : rrItem.failures) {
            if ((row.labels != null) &&
                    (row.labels.indexOf("Resurfaced") != -1)) {
                resurfacedEventsIds.put(row.id, row);
            }
        }
    }

    public static String getKey(Set<String> qualityGates) {
        return String.join(".", qualityGates);
    }

    private void addCriticalErrors(ReliabilityReportItem rrItem) {
        for (EventRow row : rrItem.failures) {
            criticalEventsIds.put(row.id, row);
        }
    }

    private void addIncreasingErrors(ReliabilityReportItem rrItem) {
        Collection<RegressionRow> regressionRows = rrItem.geIncErrors(true, true);
        for (RegressionRow row : regressionRows) {
            increasingEventsIds.put(row.id, row);
        }
    }

    private void addNewErrors(ReliabilityReportItem rrItem) {
        Collection<RegressionRow> newErrors = rrItem.getNewErrors(true, true);
        for (RegressionRow row : newErrors) {
            newEventsIds.put(row.id, row);
        }
    }

    public Set<String> getQualityGates(String id) {
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

        if (result.size() == 0) {
            result.add("NO_QUALITY_GATE_IT_SHOULD_BE_VERIFIED");
        }

        return result;
    }
}
