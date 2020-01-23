package com.overops.plugins.sonar.measures;

import com.takipi.api.client.data.event.Location;
import com.takipi.api.client.functions.output.BaseEventRow;
import com.takipi.api.client.functions.output.ReliabilityReport;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.overops.plugins.sonar.OverOpsConfigurationDataManager.overOpsEventsStatistic;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.getOverOpsByQualityGate;
import static com.overops.plugins.sonar.measures.OverOpsQualityGateStat.*;

public class OverOpsEventsStatistic implements Serializable {
  private static final long serialVersionUID = -3512666416832453373L;

  private transient OverOpsQualityGateStat overOpsQualityGateStat;
    private Stat stat = new Stat();
    private HashMap<String, StatEvent> idToStatEvent = new HashMap<>();

    public void add(StatEvent statEvent) {
        idToStatEvent.put(statEvent.eventId, statEvent);

        String key = statEvent.eventClassIdentifier;
        if (stat.get(key) == null) {
            System.out.println("      New stat for  " + key + "   qualityGate :" + statEvent.qualityGatesKey);
            stat.put(key, new ClassStat(statEvent));
        } else {
            stat.update(key, statEvent);
        }
    }

    public void setOverOpsQualityGateStat(ReliabilityReport reliabilityReport) {
        this.overOpsQualityGateStat = new OverOpsQualityGateStat(reliabilityReport);
        printQualityGateStat();
        List<StatEvent> eventAdapter = getList();

        for (StatEvent statEvent : eventAdapter) {
            overOpsEventsStatistic.add(statEvent);
        }
    }

    private List<StatEvent> getList() {

        ArrayList<StatEvent> result = new ArrayList<>();
        result.addAll(getStatEventsFromSeries(overOpsQualityGateStat.newEventsIds.values()));
        result.addAll(getStatEventsFromSeries(overOpsQualityGateStat.criticalEventsIds.values()));
        result.addAll(getStatEventsFromSeries(overOpsQualityGateStat.resurfacedEventsIds.values()));
        result.addAll(getStatEventsFromSeries(overOpsQualityGateStat.increasingEventsIds.values()));

        return result;
    }

    private <T extends BaseEventRow> Collection<StatEvent> getStatEventsFromSeries(Iterable<T> series) {
        ArrayList<StatEvent> result = new ArrayList<>();

        for (T baseEventRow : series) {
            result.add(new StatEvent(baseEventRow, overOpsQualityGateStat));
        }
        return result;
    }

    private void printQualityGateStat() {
        System.out.println(" ");
        String[] newIds = overOpsQualityGateStat.newEventsIds.keySet().toArray(new String[0]);
        System.out.println("newEventsIds [" + String.join(",", newIds) + "]");
        System.out.println(" ");
        String[] increasingIds = overOpsQualityGateStat.increasingEventsIds.keySet().toArray(new String[0]);
        System.out.println("increasingIds [" + String.join(",", increasingIds) + "]");
        System.out.println(" ");
        String[] resurfacedIds = overOpsQualityGateStat.resurfacedEventsIds.keySet().toArray(new String[0]);
        System.out.println("resurfacedIds [" + String.join(",", resurfacedIds) + "]");
        System.out.println(" ");
        String[] criticalIds = overOpsQualityGateStat.criticalEventsIds.keySet().toArray(new String[0]);
        System.out.println("criticalIds [" + String.join(",", criticalIds) + "]");
        System.out.println(" ");
    }

    public OverOpsQualityGateStat getOverOpsQualityGateStat() {
        return overOpsQualityGateStat;
    }

    public Collection<ClassStat> getStatistic() {
        return stat.values();
    }

    public Set<String> getStatisticKeys() {
        return stat.keySet();
    }

    public StatEvent getStatEventById(String issueEventId) {
        return idToStatEvent.get(issueEventId);
    }

    public int getSourceCode(StatEvent statEvent) {
        if (statEvent.qualityGates.contains(NEW_QG_MARKER)) {
            return 70;
        } else if (statEvent.qualityGates.contains(CRITICAL_QG_MARKER)) {
            return 71;
        } else if (statEvent.qualityGates.contains(RESURFACED_QG_MARKER)) {
            return 72;
        } else if (statEvent.qualityGates.contains(INCREASING_QG_MARKER)) {
            return 73;
        }
        return 0;
    }

    public static class Stat extends HashMap<String, ClassStat> implements Serializable {
      private static final long serialVersionUID = 8537754052916605788L;

    public Stat() {
        }

        public void update(String key, StatEvent statEvent) {
            ClassStat classStat = get(key);
            classStat.increment(statEvent);
            System.out.println("           Update  on " + classStat.fileName +
                    " times  = " + classStat.qualityGateToEventStat.get(statEvent.qualityGatesKey).total
                    + "   qualityGate :" + statEvent.qualityGatesKey);
        }
    }

    public static class ClassStat implements Serializable {
      private static final long serialVersionUID = -5009574971566387166L;
    public String fileName;
        public Map<String, EventInClassStat> qualityGateToEventStat;
        public Map<String, EventInClassStat> reportableQualityGateToEventStat;

        public ClassStat(StatEvent statEvent) {
            this.fileName = statEvent.eventClassIdentifier;
            qualityGateToEventStat = new HashMap<>();
            reportableQualityGateToEventStat = new HashMap<>();
            qualityGateToEventStat.put(statEvent.qualityGatesKey, new EventInClassStat(statEvent));
            updateReportableQualityGateToEventStat(statEvent);
        }

        public void increment(StatEvent statEvent) {
            EventInClassStat eventInClassStat = qualityGateToEventStat.get(statEvent.qualityGatesKey);
            if (eventInClassStat == null) {
                qualityGateToEventStat.put(statEvent.qualityGatesKey, new EventInClassStat(statEvent));
            } else {
                eventInClassStat.update(statEvent);
            }
            updateReportableQualityGateToEventStat(statEvent);
        }

        private void updateReportableQualityGateToEventStat(StatEvent statEvent) {
            String qualityGateKey = statEvent.qualityGatesKey;
            OverOpsMetrics.OverOpsMetric overOpsMetric = getOverOpsByQualityGate(qualityGateKey);
            if (overOpsMetric != null) {
                if (overOpsMetric.isCombo()) {
                    for (String gate : overOpsMetric.qualityGate) {
                        OverOpsMetrics.OverOpsMetric reportableOverOpsMetric = getOverOpsByQualityGate(gate);
                        if (reportableOverOpsMetric != null) {
                            putReportableQualityGateData(reportableOverOpsMetric.qualityGateKey, statEvent);
                        }
                    }
                } else {
                    putReportableQualityGateData(qualityGateKey, statEvent);
                }
            }
        }

        private void putReportableQualityGateData(String qualityGateKey, StatEvent statEvent) {
            EventInClassStat resultEventInClassStat = reportableQualityGateToEventStat.get(qualityGateKey);
            if (resultEventInClassStat != null) {
                resultEventInClassStat.update(statEvent);
            } else {
                reportableQualityGateToEventStat.put(qualityGateKey, new EventInClassStat(statEvent));
            }
        }
    }

    public static class EventInClassStat implements Serializable {
    private static final long serialVersionUID = 5500890325486878171L;
    public int total;
        public Map<Integer, LineStat> lineToLineStat = new HashMap<>();

        public EventInClassStat(StatEvent statEvent) {
            total = 1;
            lineToLineStat.put(statEvent.eventMethodPosition, new LineStat(statEvent));
        }

        public void update(StatEvent statEvent) {
            total++;
            LineStat lineStat = lineToLineStat.get(statEvent.eventMethodPosition);
            if (lineStat == null) {
                lineToLineStat.put(statEvent.eventMethodPosition, new LineStat(statEvent));
            } else {
                lineStat.update(statEvent);
            }
        }
    }

    public static class LineStat implements Serializable {
    private static final long serialVersionUID = -7959327603311558048L;
    public int total;
        public StatEvent event;

        public LineStat(StatEvent event) {
            total = 1;
            this.event = event;
        }

        public void update(StatEvent event) {
            total++;
            this.event = event;
        }
    }

    public static class StatEvent implements Serializable {
    private static final long serialVersionUID = -7845347872152704070L;
    public final String eventId;
        public final String eventSummary;
        public final String eventClassIdentifier;
        public final int eventMethodPosition;
        public final String similar_event_ids;
        public final List<String> stack_frames;
        public final Set<String> qualityGates;
        public final String qualityGatesKey;

        public StatEvent(BaseEventRow row, OverOpsQualityGateStat overOpsQualityGateStat) {
            this.eventId = row.id;
            this.eventSummary = row.summary;
            this.similar_event_ids = row.similar_event_ids;
            this.qualityGates = overOpsQualityGateStat.getQualityGates(eventId);

            List<Location> stack_frames = row.stack_frames;
            if (stack_frames != null && stack_frames.size() > 0) {
                Location location = stack_frames.get(0);
                this.eventMethodPosition = location.original_line_number;
                this.eventClassIdentifier = location.class_name;
                this.stack_frames = stack_frames.stream().map(l -> l.prettified_name).collect(Collectors.toList());
            } else  {
                this.eventMethodPosition = 1;
                this.eventClassIdentifier = "no class identifier";
                this.stack_frames = new ArrayList<>();
            }

            this.qualityGatesKey = getKey(qualityGates);

            printValues();
        }

        private void printValues() {
            System.out.println(" ----->>>> " + eventClassIdentifier + " : id [" + eventId + "] L <" + eventMethodPosition + ">  S {" + eventSummary + "}");
            System.out.println("                                     QG " + qualityGatesKey);
            System.out.println("");
        }
    }
}
