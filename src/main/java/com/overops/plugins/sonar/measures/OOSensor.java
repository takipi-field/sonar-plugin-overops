package com.overops.plugins.sonar.measures;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.CaughtExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.HTTPErrors;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.LogErrorCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.SwallowedExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.Total_Errors;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.UncaughtExceptionCount;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.api.client.util.regression.RegressionInput;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.File;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class OOSensor implements Sensor {

	private static final Logger LOGGER = Loggers.get(OOSensor.class);

	public static EventsResult eventList;
	public String caughtException = "Caught Exception";
	public String swallowedException = "Swallowed Exception";
	public String totalErrors = "Total Errors";
	public String uncaughtException = "Uncaught Exception";
	public String loggedError = "Logged Error";
	public String customEvent = "Custom Event";
	public String httpError = "HTTP Error";

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("OverOps sensor calling the Summarized View API and setting up the default Measures");
	}

	@Override
	public void execute(SensorContext context) {
		Configuration config = context.config();
		String envIdKey = config.get(OverOpsProperties.OO_ENVID).orElse(null);
		String appHost = config.get(OverOpsProperties.OO_URL).orElse("https://api.overops.com");
		String apiKey = config.get(OverOpsProperties.APIKEY).orElse(null);
		String dep_name = config.get(OverOpsProperties.DEP_NAME).orElse(null);
		String app_name = config.get(OverOpsProperties.APP_NAME).orElse(null);

		if (OverOpsProperties.APIKEY == null) {
			throw new IllegalStateException("APIKey is not filled in correctly");
		}

		RemoteApiClient apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(apiKey)
				.setHostname(appHost).build();

		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, envIdKey, "All Events");

		Instant today = Instant.now();
		long days = context.config().getLong(OverOpsProperties.DAYS).orElse(1l);
		Instant from = today.minus(days, ChronoUnit.DAYS);

		// use the number inputted by the user default is 1 day
		EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder().setServiceId(envIdKey.toUpperCase())
				.setFrom(from.toString()).setTo(today.toString()).setViewId(view.id).setVolumeType(VolumeType.all)
				.build();

		Response<EventsResult> eventsResponse = apiClient.get(eventsVolumeRequest);

		HashMap<String, Integer> exceptionCounts = prepareMapDefault();

		// prepare the map values, to set the values of the Measures
		if (eventsResponse.data.events == null) {
			LOGGER.info("Null event");
			setContexts(context, exceptionCounts);
		} else if (eventsResponse.isBadResponse()) {
			throw new IllegalStateException("Failed getting events.");
		} else {
			eventList = eventsResponse.data;
			exceptionCounts = getAndCountExceptions();
			setContexts(context, exceptionCounts);
		}
	}

	public void setContexts(SensorContext context, HashMap<String, Integer> exceptionCounts) {
		context.<Integer>newMeasure().forMetric(CaughtExceptionCount).on(context.module())
				.withValue(exceptionCounts.get(caughtException)).save();

		context.<Integer>newMeasure().forMetric(SwallowedExceptionCount).on(context.module())
				.withValue(exceptionCounts.get(swallowedException)).save();

		context.<Integer>newMeasure().forMetric(Total_Errors).on(context.module())
				.withValue(exceptionCounts.get(totalErrors)).save();

		context.<Integer>newMeasure().forMetric(UncaughtExceptionCount).on(context.module())
				.withValue(exceptionCounts.get(uncaughtException)).save();

		context.<Integer>newMeasure().forMetric(LogErrorCount).on(context.module())
				.withValue(exceptionCounts.get(loggedError)).save();

		context.<Integer>newMeasure().forMetric(HTTPErrors).on(context.module())
				.withValue(exceptionCounts.get(httpError)).save();

	}

	public HashMap<String, Integer> getAndCountExceptions() {
		// counts all the relevant errors
		HashMap<String, Integer> exceptions = prepareMapDefault();
		exceptions.put(totalErrors, eventList.events.size());
		if (eventList == null) {
			return exceptions;
		}
		for (int i = 0; i < eventList.events.size(); ++i) {
			if (exceptions.containsKey(eventList.events.get(i).type)) {
				int count = exceptions.remove(eventList.events.get(i).type);
				exceptions.put(eventList.events.get(i).type, ++count);
			} else {
				exceptions.put(eventList.events.get(i).type, 1);
			}
		}
		return exceptions;
	}

	public HashMap<String, Integer> prepareMapDefault() {
		HashMap<String, Integer> exceptions = new HashMap<>();
		exceptions.put(caughtException, 0);
		exceptions.put(swallowedException, 0);
		exceptions.put(totalErrors, 0);
		exceptions.put(uncaughtException, 0);
		exceptions.put(loggedError, 0);
		exceptions.put(customEvent, 0);
		exceptions.put(httpError, 0);
		return exceptions;
	}
}
