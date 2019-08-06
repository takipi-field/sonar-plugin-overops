package com.overops.plugins.sonar.measures;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.CustomExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.HTTPErrors;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.LogErrorCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.SwallowedExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.Total_Unique_Errors;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.UncaughtExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.event_list_size;

import java.util.HashMap;

import com.overops.plugins.sonar.converter.OverOpsPluginConfiguration;
import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.internal.apachecommons.lang.NullArgumentException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class OOSensor implements Sensor {

	private static final Logger LOGGER = Loggers.get(OOSensor.class);

	public static EventsResult eventList;

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("OverOps sensor calling the Summarized View API and setting up the default Measures");
	}

	public static EventsResult getEventResult() {
		return eventList;
	}

	public static void setEventsResult(EventsResult eventListR) {
		eventList = eventListR;
	}

	@Override
	public void execute(SensorContext context) {
		Configuration config = context.config();
		String envIdKey = config.get(OverOpsProperties.OO_ENVID).orElse(null);
		String appHost = config.get(OverOpsProperties.OO_URL).orElse(null);
		String apiKey = config.get(OverOpsProperties.APIKEY).orElse(null);

		if (OverOpsProperties.APIKEY == null) {
			throw new IllegalStateException("APIKey is not filled in correctly");
		}

		RemoteApiClient apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(apiKey)
				.setHostname(appHost).build();

		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, envIdKey, "All Events");

		DateTimeFormatter dtf = ISODateTimeFormat.dateTime().withZoneUTC();
		DateTime to = DateTime.now();
		
		//use the number inputted by the user default is 1 day
		DateTime from = to.minusDays(context.config().getInt(OverOpsProperties.DAYS).orElse(1));

		EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(envIdKey.toUpperCase())
				.setFrom(from.toString(dtf)).setTo(to.toString(dtf)).setViewId(view.id).setVolumeType(VolumeType.all)
				.build();

		Response<EventsResult> eventsResponse = apiClient.get(eventsVolumeRequest);

		if (eventsResponse.isBadResponse()) {
			throw new IllegalStateException("Failed getting events.");
		}
		setEventsResult(eventsResponse.data);

		HashMap<String, Integer> exceptionCounts = getAndCountExceptions();

		context.<Integer>newMeasure().forMetric(UncaughtExceptionCount).on(context.module())
				.withValue(exceptionCounts.get("Caught Exception")).save();
		context.<Integer>newMeasure().forMetric(SwallowedExceptionCount).on(context.module())
				.withValue(exceptionCounts.get("Swallowed Exception")).save();
		context.<Integer>newMeasure().forMetric(event_list_size).on(context.module())
				.withValue(exceptionCounts.get("Total Errors")).save();
		context.<Integer>newMeasure().forMetric(Total_Unique_Errors).on(context.module())
				.withValue(exceptionCounts.get("Uncaught Exception")).save();
		context.<Integer>newMeasure().forMetric(LogErrorCount).on(context.module())
				.withValue(exceptionCounts.get("Logged Error")).save();
		context.<Integer>newMeasure().forMetric(CustomExceptionCount).on(context.module())
				.withValue(exceptionCounts.get("Custom Event")).save();
		context.<Integer>newMeasure().forMetric(HTTPErrors).on(context.module())
				.withValue(exceptionCounts.get("HTTP Error")).save();
	}

	public HashMap<String, Integer> getAndCountExceptions() {
		// counts all the relevant errors
		HashMap<String, Integer> exceptions = prepareMapDefault();
		for (int i = 0; i < eventList.events.size(); ++i) {
			if (exceptions.containsKey(eventList.events.get(i).type)) {
				int count = exceptions.remove(eventList.events.get(i).type);
				exceptions.put(eventList.events.get(i).type, ++count);
			} else {
				exceptions.put(eventList.events.get(i).type, 1);
			}
			LOGGER.info("Exception Type: " + eventList.events.get(i).type);
		}
		// incase there are no errors of a type I add them
		if (!exceptions.containsKey("Caught Exception")) {
			exceptions.put("Caught Exception", 0);
		}
		if (!exceptions.containsKey("Swallowed Exception")) {
			exceptions.put("Swallowed Exception", 0);
		}
		if (!exceptions.containsKey("Custom Event")) {
			exceptions.put("Custom Event", 0);
		}
		if (!exceptions.containsKey("Logged Error")) {
			exceptions.put("Logged Error", 0);
		}
		if (!exceptions.containsKey("Uncaught Exception")) {
			exceptions.put("Uncaught Exception", 0);
		}
		if (!exceptions.containsKey("HTTP Error")) {
			exceptions.put("HTTP Error", 0);
		}
		if (eventList == null) {
			exceptions.put("Total Errors", 0);
		} else {
			exceptions.put("Total Errors", eventList.events.size());
		}
		return exceptions;
	}

	public HashMap<String, Integer> prepareMapDefault(){
		HashMap<String, Integer> exceptions = new HashMap<>();
		exceptions.put("Caught Exception", 0);
		exceptions.put("SwallowedException",0);
		exceptions.put("Total Errors",0);
		exceptions.put("Uncaught Exception",0);
		exceptions.put("Logged Error",0);
		exceptions.put("Custom Event",0);
		exceptions.put("HTTP Error",0);
		return exceptions;
	}
}
