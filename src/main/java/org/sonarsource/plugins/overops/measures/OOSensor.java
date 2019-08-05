package org.sonarsource.plugins.overops.measures;

import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.event_list_size;

import java.util.HashMap;

import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.Total_Unique_Errors;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.UncaughtExceptionCount;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.SwallowedExceptionCount;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.LogErrorCount;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.CustomExceptionCount;

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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.plugins.overops.settings.OverOpsProperties;

public class OOSensor implements Sensor {

	private static final Logger LOGGER = Loggers.get(OOSensor.class);

	public static EventsResult eventList;

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("OverOps sensor calling OO_TALKER");
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
		DateTime from = to.minusDays(14);

		EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(envIdKey.toUpperCase())
				.setFrom(from.toString(dtf)).setTo(to.toString(dtf)).setViewId(view.id).setVolumeType(VolumeType.all)
				.build();

		Response<EventsResult> eventsResponse = apiClient.get(eventsVolumeRequest);
		if (eventsResponse.isBadResponse())
			throw new IllegalStateException("Failed getting events.");
		setEventsResult(eventsResponse.data);

		HashMap<String, Integer> exceptionCounts = getAndCountExceptions();

		context.<Integer>newMeasure().forMetric(UncaughtExceptionCount).on(context.module())
				.withValue(exceptionCounts.get("Caught Exception")).save();
		context.<Integer>newMeasure().forMetric(SwallowedExceptionCount).on(context.module())
				.withValue(exceptionCounts.get("Swallowed Exception")).save();
		context.<Integer>newMeasure().forMetric(event_list_size).on(context.module())
				.withValue(exceptionCounts.get("Total Errors")).save();
		context.<Integer>newMeasure().forMetric(Total_Unique_Errors).on(context.module())
				.withValue(exceptionCounts.get("UnCaught Exception")).save();
		context.<Integer>newMeasure().forMetric(LogErrorCount).on(context.module())
				.withValue(exceptionCounts.get("Logged Error")).save();
		context.<Integer>newMeasure().forMetric(CustomExceptionCount).on(context.module())
				.withValue(exceptionCounts.get("Custom Event")).save();

	}

	public HashMap<String, Integer> getAndCountExceptions() {
		HashMap<String, Integer> exceptions = new HashMap<>();
		for (int i = 0; i < eventList.events.size(); ++i) {
			if (exceptions.containsKey(eventList.events.get(i).type)) {
				int count = exceptions.remove(eventList.events.get(i).type);
				exceptions.put(eventList.events.get(i).type, ++count);
			} else {
				exceptions.put(eventList.events.get(i).type, 1);
			}
			LOGGER.info("Exception Type: " + eventList.events.get(i).type);
		}
		if (!exceptions.containsKey("Caught Exception")) {
			exceptions.put("Caught Exception", 0);
		} else if (!exceptions.containsKey("Swallowed Exception")) {
			exceptions.put("Swallowed Exception", 0);
		} else if (!exceptions.containsKey("Custom Event")) {
			exceptions.put("Custom Event", 0);
		} else if (!exceptions.containsKey("Logged Error")) {
			exceptions.put("Logged Error", 0);
		}else if(!exceptions.containsKey("UnCaught Exception")){
			exceptions.put("UnCaught Exception", 0);
		}
		exceptions.put("Total Errors", eventList.events.size());
		return exceptions;
	}

}
