package com.overops.plugins.sonar;

import com.overops.plugins.sonar.model.Event;
import com.overops.plugins.sonar.model.EventsJson;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface EventSensor extends Sensor {

    void initConfig(SensorContext context);

    void validateConfig();

    EventsJson addIssuesAndMetrics(SensorContext context, Map.Entry<String, ArrayList<Event>> fileEvent);

    void saveFileEvents(HashMap<String, ArrayList<Event>> fileEvents) throws IOException;

    HashMap<String, ArrayList<Event>> mapFileEvents(Series<SeriesRow> events);
}
