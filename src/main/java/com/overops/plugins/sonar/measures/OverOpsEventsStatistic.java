package com.overops.plugins.sonar.measures;

import com.takipi.api.client.result.event.EventResult;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.overops.plugins.sonar.measures.OverOpsQualityGateStat.getKey;

public class OverOpsEventsStatistic {
    private static final Logger LOGGER = Loggers.get(OverOpsEventsStatistic.class);
    private OverOpsQualityGateStat overOpsQualityGateStat;
    private Stat stat = new Stat();

    public void add(EventResult event) {
        StatEvent statEvent = new StatEvent(event, overOpsQualityGateStat.getQualityGates(event.id));
        String key = statEvent.getClassName();
        if (stat.get(key) == null) {
            LOGGER.info("      New stat for  " + key + "   qualityGate :" + statEvent.qualityGatesKey);
            stat.put(key, new ClassStat(statEvent));
        } else {
            stat.update(key, statEvent);
        }
    }

    public void setOverOpsQualityGateStat(OverOpsQualityGateStat overOpsQualityGateStat) {
        this.overOpsQualityGateStat = overOpsQualityGateStat;
    }

    public Collection<ClassStat> getStatistic() {
        return stat.values();
    }

    public static class Stat extends HashMap<String, ClassStat> {

        public void update(String key, StatEvent statEvent) {
            ClassStat classStat = get(key);
            classStat.increment(statEvent);
            LOGGER.info("           Update  on " + classStat.fileName +
                    "  type [ " + statEvent.getType() +
                    " ]  times  = " + classStat.qualityGateToEventStat.get(statEvent.qualityGatesKey).total
                    + "   qualityGate :" + statEvent.qualityGatesKey);
        }
    }

    public static class ClassStat {
        public String fileName;
        public Map<String, EventInClassStat> qualityGateToEventStat;

        public ClassStat(StatEvent statEvent) {
            this.fileName = statEvent.getClassName();
            qualityGateToEventStat = new HashMap<>();
            qualityGateToEventStat.put(statEvent.qualityGatesKey, new EventInClassStat(statEvent));
        }

        public void increment(StatEvent statEvent) {
            EventInClassStat eventInClassStat = qualityGateToEventStat.get(statEvent.qualityGatesKey);
            if (eventInClassStat == null) {
                qualityGateToEventStat.put(statEvent.qualityGatesKey, new EventInClassStat(statEvent));
            } else {
                eventInClassStat.update(statEvent);
            }
        }
    }

    public static class EventInClassStat {
        public int total;
        public Map<Integer, LineStat> lineToLineStat = new HashMap<>();

        public EventInClassStat(StatEvent statEvent) {
            total = 1;
            lineToLineStat.put(getLine(statEvent), new LineStat(statEvent));
        }

        public void update(StatEvent statEvent) {
            total++;
            LineStat lineStat = lineToLineStat.get(getLine(statEvent));
            if (lineStat == null) {
                lineToLineStat.put(getLine(statEvent), new LineStat(statEvent));
            } else {
                lineStat.update(statEvent);
            }
        }
    }

    public static class LineStat {
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

    public static int getLine(StatEvent statEvent) {
        return statEvent.getMethodPosition();
    }

    public static class StatEvent {
        public EventResult eventResult;
        public Set<String> qualityGates;
        public String qualityGatesKey;

        public StatEvent(EventResult eventResult, Set<String> qualityGates) {
            this.eventResult = eventResult;
            this.qualityGates = qualityGates;
            this.qualityGatesKey = getKey(qualityGates);
        }

        public String getType() {
            return eventResult.type;
        }

        public String getClassName() {
            return eventResult.error_location.class_name;
        }

        private int getMethodPosition() {
            return eventResult.error_location.method_position;
        }
    }
}