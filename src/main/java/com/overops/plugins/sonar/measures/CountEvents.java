package com.overops.plugins.sonar.measures;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.rules.RuleType;

public class CountEvents {
    EventsResult events;// overops api data

    HashMap<String, Metric<Integer>> typeToMetricMap;

    public final String caughtException = "Caught Exception";
    public final String swallowedException = "Swallowed Exception";
    public final String uncaughtException = "Uncaught Exception";
    public final String loggedError = "Logged Error";
    public final String customEvent = "Custom Event";
    public final String httpError = "HTTP Error";
    public final String criticalExceptions = "Critical Exception";

    public CountEvents(EventsResult result) {
        events = result;
        typeToMetricMap = ooErrorTypeToMetricSetUp();
    }

    // prepares map so no Metrics<Integer> are null which causes sonar to fail a
    // scan
    public HashMap<String, Integer> prepareMap(HashMap<String, Integer> map) {
        map.put(caughtException, 0);
        map.put(swallowedException, 0);
        map.put(uncaughtException, 0);
        map.put(loggedError, 0);
        map.put(customEvent, 0);
        map.put(httpError, 0);
        map.put(criticalExceptions, 0);
        return map;
    }

    // classname to methodname to count
    // type happen only counts
    public HashMap<String, HashMap<String, Integer>> countClassErrors() {
        HashMap<String, HashMap<String, Integer>> classErrorCounts = new HashMap<String, HashMap<String, Integer>>();
        for (EventResult result : events.events) {
            // classes first event in list ex result.error_location.class_name=
            // COM.EMPIRE.SHOPPINGCART.MANAGER.CUSTOMEVENTMANAGER reduced to just classname
            // no extension

            String eventClassNameShortened = "";
            if (result.error_location.class_name.contains(".")) {
                eventClassNameShortened = result.error_location.class_name.substring(
                        result.error_location.class_name.lastIndexOf('.') + 1,
                        result.error_location.class_name.length());
            } else {
                eventClassNameShortened = result.error_location.class_name;
            }
            if (!classErrorCounts.containsKey(eventClassNameShortened)) {
                HashMap<String, Integer> errorToCount = new HashMap<>();
                errorToCount = prepareMap(errorToCount);
                int count = errorToCount.remove(result.type);
                count++;
                errorToCount.put(result.type, count);
                classErrorCounts.put(eventClassNameShortened, errorToCount);
            } else {
                HashMap<String, Integer> errorCount = classErrorCounts.remove(eventClassNameShortened);
                // iterative variable has the same class as one already found and if it has the
                // same type of error increment count
                int count = errorCount.remove(result.type);
                count++;
                errorCount.put(result.type, count);
                classErrorCounts.put(eventClassNameShortened, errorCount);
            }
        }
        return classErrorCounts;
    }

    // this sets up the map that will give the OO event.type to the Sonar Metric
    public HashMap<String, Metric<Integer>> ooErrorTypeToMetricSetUp() {
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

}