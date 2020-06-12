package com.overops.plugins.sonar;

import com.overops.plugins.sonar.model.EventsJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Not pretty; but we need a way to cache events across Sensors
 */
public class EventDataStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDataStore.class);
    private static final EventDataStore INSTANCE = new EventDataStore();

    public static EventDataStore instance(){
        return INSTANCE;
    }

    private List<EventsJson> data;

    private EventDataStore() {
        this.data = new ArrayList<>();
    }

    public List<EventsJson> getData() {
        return data;
    }

    public void setData(List<EventsJson> data){
        this.data = data;
    }
}
