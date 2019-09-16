package com.overops.plugins.sonar.measures;

import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.*;

import static com.overops.plugins.sonar.measures.OverOpsSensor.convertInputFileToClassName;

public class EventsStatistic {
    private EventsResult events;
    private HashMap<String, Metric<Integer>> typeToMetricMap;

    public final String caughtException = "Caught Exception";
    public final String swallowedException = "Swallowed Exception";
    public final String uncaughtException = "Uncaught Exception";
    public final String loggedError = "Logged Error";
    public final String customEvent = "Custom Event";
    public final String httpError = "HTTP Error";
    public final String criticalExceptions = "Critical Exception";

    private Stat stat = new Stat();

    public EventsStatistic(EventsResult result) {
        events = result;
        typeToMetricMap = initTypeToMetricMap();
    }

    public HashMap<String, Metric<Integer>> initTypeToMetricMap() {
        HashMap<String, Metric<Integer>> exceptions = new HashMap<>();
        exceptions.put(caughtException, OverOpsMetrics.CaughtExceptionCount);
        exceptions.put(customEvent, OverOpsMetrics.CustomExceptionCount);
        exceptions.put(httpError, OverOpsMetrics.HTTPErrors);
        exceptions.put(loggedError, OverOpsMetrics.LogErrorCount);
        exceptions.put(swallowedException, OverOpsMetrics.SwallowedExceptionCount);
        exceptions.put(uncaughtException, OverOpsMetrics.UncaughtExceptionCount);
        exceptions.put(criticalExceptions, OverOpsMetrics.CriticalExceptionCount);
        return exceptions;
    }

    private static final Logger LOGGER = Loggers.get(EventsStatistic.class);

    public Metric<Integer> getMetric(String exceptionType) {
         return typeToMetricMap.get(exceptionType);
    }

    public void add(InputFile file, EventResult event) {
        String key = convertInputFileToClassName(file);
        if (stat.get(key) == null) {
            LOGGER.info("      New stat for  " + key);
            stat.put(key, new ClassStat(file, event));
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
            LOGGER.info("           Update  on " + classStat.file.filename() +
                    "  type [ " + event.type +
                    " ]  times  = " + classStat.typeToEventStat.get(event.type).total);
        }
    }

    public static class ClassStat {
        public InputFile file;
        public Map<String, EventInClassStat> typeToEventStat;

        public ClassStat(InputFile file, EventResult event) {
            this.file = file;
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
        EventResult event;

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