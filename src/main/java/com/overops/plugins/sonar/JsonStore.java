package com.overops.plugins.sonar;

import java.util.List;

public class JsonStore {

	public static final String STORE_FILE = "overops_db.json";

	List<EventsJson> eventsJson;

	public void setEventsJson(List<EventsJson> eventsJson) {
		this.eventsJson = eventsJson;
	}
	public List<EventsJson> getEventsJson() {
		return eventsJson;
	}
}