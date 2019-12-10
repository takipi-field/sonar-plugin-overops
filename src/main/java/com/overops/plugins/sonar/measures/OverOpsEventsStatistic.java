package com.overops.plugins.sonar.measures;

import com.takipi.api.client.functions.output.ReliabilityReport;
import com.takipi.api.client.functions.output.ReliabilityReportRow;
import com.takipi.api.client.functions.output.Series;

import java.io.Serializable;
import java.util.*;

import static com.overops.plugins.sonar.OverOpsPlugin.overOpsEventsStatistic;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.OverOpsMetric.getOverOpsByQualityGate;
import static com.overops.plugins.sonar.measures.OverOpsQualityGateStat.getKey;

public class OverOpsEventsStatistic implements Serializable {
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
        List<StatEvent> eventAdapter = getList(reliabilityReport);

        for (StatEvent statEvent : eventAdapter) {
            overOpsEventsStatistic.add(statEvent);
        }
    }

    private List<StatEvent> getList(ReliabilityReport reliabilityReport) {

        ArrayList<StatEvent> result = new ArrayList<>();
        for (Map.Entry<ReliabilityReportRow.Header, ReliabilityReport.ReliabilityReportItem> entry : reliabilityReport.items.entrySet()) {
            ReliabilityReport.ReliabilityReportItem reliabilityReportItem = entry.getValue();
            result.addAll(getStatEventsFromSeries(reliabilityReportItem.failures));
            result.addAll(getStatEventsFromSeries(reliabilityReportItem.errors));
            result.addAll(getStatEventsFromSeries(reliabilityReportItem.regressions));
        }

        return result;
    }

    private Collection<StatEvent> getStatEventsFromSeries(Series<?> series) {
        ArrayList<StatEvent> result = new ArrayList<>();

        int size = series.size();
        for (int i = 0; i < size; i++) {
            result.add(new StatEvent(series, i, overOpsQualityGateStat));
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

    public static class Stat extends HashMap<String, ClassStat> implements Serializable {

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
        public final String eventId;
        public final String eventSummary;
        public final String eventClassIdentifier;
        public final int eventMethodPosition;
        public final String similar_event_ids;
        public final List<String> stack_frames;
        public final Set<String> qualityGates;
        public final String qualityGatesKey;

        public StatEvent(Series<?> series, int index, OverOpsQualityGateStat overOpsQualityGateStat) {

            this.eventId = series.getString("id", index);
            this.eventMethodPosition = getLineNumber(series, index);
            this.eventClassIdentifier = getClassName(series, index);
            this.eventSummary = series.getString("name", index);
            this.stack_frames = getStackFrames(series, index);
            this.similar_event_ids = series.getString("similar_event_ids", index);
            this.qualityGates = overOpsQualityGateStat.getQualityGates(eventId);
            this.qualityGatesKey = getKey(qualityGates);

            printValues();
        }

        private void printValues() {
            System.out.println(" ----->>>> " + eventClassIdentifier + " : id [" + eventId + "] L <" + eventMethodPosition + ">  S {" + eventSummary + "}");
            System.out.println("                                     QG " + qualityGatesKey);
            System.out.println("");
        }

        private int getLineNumber(Series<?> series, int index) {
            //TODO retrieve line number
            return 1;
        }

        private String getClassName(Series<?> series, int index) {
            //TODO retrieve full path class name
            return series.getString("entry_point_name", index);
        }

        private List<String> getStackFrames(Series<?> series, int index) {
            //TODO retrieve StackFrames
            ArrayList<String> result = new ArrayList<>();
            result.add("TODO retrieve StackFrames");
            return result;
        }
    }
}
