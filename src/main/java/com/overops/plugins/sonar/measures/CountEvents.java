package com.overops.plugins.sonar.measures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;

public class CountEvents {
    EventsResult events;// overops api data

    HashMap<String, Metric<Integer>> typeToMetricMap;
    HashMap<String, ArrayList<String>> classNameToMethodNameMap;
    public final String caughtException = "Caught Exception";
    public final String swallowedException = "Swallowed Exception";
    public final String uncaughtException = "Uncaught Exception";
    public final String loggedError = "Logged Error";
    public final String customEvent = "Custom Event";
    public final String httpError = "HTTP Error";

    public CountEvents(EventsResult result) {
        events = result;
        typeToMetricMap = ooErrorTypeToMetricSetUp();
        classNameToMethodNameMap = new HashMap<>();
    }

    public void classToMethodBuilder() {
        String shortEnedName;
        for (EventResult event : events.events) {
            if(event.error_location.class_name.contains(".")){
                int lastPeriod = event.error_location.class_name.lastIndexOf('.');
                shortEnedName = event.error_location.class_name.substring(lastPeriod+1, event.error_location.class_name.length());
            }else{
                shortEnedName = event.error_location.class_name;
            }
            if (classNameToMethodNameMap.containsKey(shortEnedName)) {
                ArrayList<String> methodNames = classNameToMethodNameMap.remove(shortEnedName);
                methodNames.add(event.error_location.method_name);
                classNameToMethodNameMap.put(shortEnedName, methodNames);
            } else {
                ArrayList<String> methodNames = new ArrayList<>();
                methodNames.add(event.error_location.method_name);
                classNameToMethodNameMap.put(shortEnedName, methodNames);
            }
        }
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
        return map;
    }

    // This is for the project level view, the total Metric values of the project
    public HashMap<String, Integer> countAllEventTypes() {
        HashMap<String, Integer> errorCounts = new HashMap<>();
        prepareMap(errorCounts);
        // counts all the relevant errors
        for (EventResult event : events.events) {
            if (errorCounts.containsKey(event.type)) {
                int count = errorCounts.remove(event.type);
                errorCounts.put(event.type, ++count);
            } else {
                errorCounts.put(event.type, 1);
            }
        }
        return errorCounts;
    }

    // This method calculates how many times the event with the same class name and
    // type happen only counts
    public HashMap<String, HashMap<String, Integer>> countClassErrors() {
        HashMap<String, HashMap<String, Integer>> classErrorCounts = new HashMap<String, HashMap<String, Integer>>();
        for (EventResult result : events.events) {
            // classes first event in list ex result.error_location.class_name=
            // COM.EMPIRE.SHOPPINGCART.MANAGER.CUSTOMEVENTMANAGER reduced to just classname
            // no extension
            String eventClassNameShortened = result.error_location.class_name.substring(result.error_location.class_name.lastIndexOf('.') + 1, result.error_location.class_name.length());
            if (!classErrorCounts.containsKey(eventClassNameShortened)) {
                HashMap<String, Integer> errorToCount = new HashMap<>();
                errorToCount = prepareMap(errorToCount);
                int count = errorToCount.remove(result.type);
                errorToCount.put(result.type, ++count);
                classErrorCounts.put(eventClassNameShortened, errorToCount);
            } else {
                HashMap<String, Integer> errorCount = classErrorCounts.remove(eventClassNameShortened);
                // iterative variable has the same class as one already found and if it has the
                // same type of error increment count
                int count = errorCount.remove(result.type);
                errorCount.put(result.type, ++count);
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
        return exceptions;
    }

}