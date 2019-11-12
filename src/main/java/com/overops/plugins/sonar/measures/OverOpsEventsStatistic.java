package com.overops.plugins.sonar.measures;

import com.takipi.api.client.result.event.EventResult;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OverOpsEventsStatistic {
    private static final Logger LOGGER = Loggers.get(OverOpsEventsStatistic.class);
    private Stat stat = new Stat();

    public void add(EventResult event) {
        String key = event.error_location.class_name;
        if (stat.get(key) == null) {
            LOGGER.info("      New stat for  " + key);
            stat.put(key, new ClassStat(event));
        } else {
            stat.update(key, event);
        }
    }

    public Collection<ClassStat> getStatistic() {
        return stat.values();
    }

    public static class Stat extends HashMap<String, ClassStat> {

        public void update(String key, EventResult event) {
            ClassStat classStat = get(key);
            classStat.increment(event);
            LOGGER.info("           Update  on " + classStat.fileName +
                    "  type [ " + event.type +
                    " ]  times  = " + classStat.typeToEventStat.get(event.type).total);
        }
    }

    public static class ClassStat {
        public String fileName;
        public Map<String, EventInClassStat> typeToEventStat;

        public ClassStat(EventResult event) {
            this.fileName = event.error_location.class_name;
            typeToEventStat = new HashMap<>();
            typeToEventStat.put(event.type, new EventInClassStat(event));
        }

        public void increment(EventResult event) {
            EventInClassStat eventInClassStat = typeToEventStat.get(event.type);
            if (eventInClassStat == null) {
                typeToEventStat.put(event.type, new EventInClassStat(event));
            } else {
                eventInClassStat.update(event);
            }
        }
    }

    public static class EventInClassStat {
        public int total;
        public Map<Integer, LineStat> lineToLineStat = new HashMap<>();

        public EventInClassStat(EventResult event) {
            total = 1;
            lineToLineStat.put(getLine(event), new LineStat(event));
        }

        public void update(EventResult event) {
            total++;
            LineStat lineStat = lineToLineStat.get(getLine(event));
            if (lineStat == null) {
                lineToLineStat.put(getLine(event), new LineStat(event));
            } else {
                lineStat.update(event);
            }
        }
    }

    public static class LineStat {
        public int total;
        public EventResult event;

        public LineStat(EventResult event) {
            total = 1;
            this.event = event;
        }

        public void update(EventResult event) {
            total++;
            this.event = event;
        }
    }

    public static int getLine(EventResult event) {
        return event.error_location.method_position;
    }
}