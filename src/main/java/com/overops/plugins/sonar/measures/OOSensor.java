package com.overops.plugins.sonar.measures;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.CaughtExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.HTTPErrors;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.LogErrorCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.UncaughtExceptionCount;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;

import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.measures.Metric;

/*
*This class is responsible for calling anythign relating to pulling OverOps data and aggregating some counts
*this classes lifecycle is heavily limited so anything brought in that is not saved will be gone before Sonar begins to display data
*currently saving onto the context.module(). On a file is preferred because it will persist but as of now we dont have line numbers.
*/
public class OOSensor implements Sensor {

	private static final Logger LOGGER = Loggers.get(OOSensor.class);
	public EventsResult eventList;

	public EventsVolumeRequest eventsVolumeRequest;
	// the event type labels
	public final String caughtException = "Caught Exception";
	public final String swallowedException = "Swallowed Exception";
	public final String uncaughtException = "Uncaught Exception";
	public final String loggedError = "Logged Error";
	public final String customEvent = "Custom Event";
	public final String httpError = "HTTP Error";

	public HashMap<String, Integer> exceptions;

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name(
				"OverOps sensor calling the Summarized View API with customer configuration and setting up the default Measures");
	}

	@Override
	public void execute(SensorContext context) {

		Configuration config = context.config();
		String envIdKey = config.get(OverOpsProperties.OO_ENVID).orElse(null);
		String appHost = config.get(OverOpsProperties.OO_URL).orElse("https://api.overops.com");
		String apiKey = config.get(OverOpsProperties.APIKEY).orElse(null);
		String dep_name = config.get(OverOpsProperties.DEP_NAME).orElse(null);
		String app_name = config.get(OverOpsProperties.APP_NAME).orElse(null);

		if (apiKey == null || envIdKey == null) {
			return;
		}

		RemoteApiClient apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(apiKey)
				.setHostname(appHost).build();

		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, envIdKey, "All Events");

		Instant today = Instant.now();
		long days = config.getLong(OverOpsProperties.DAYS).orElse(1l);
		Instant from = today.minus(days, ChronoUnit.DAYS);

		// depending on what the user is pulling in and what they have specified I
		// handle the seperate cases
		if (dep_name == null && app_name == null) {
			// no app name and deployment just the whole environment
			eventsVolumeRequest = buildEventsVolumeRequest(envIdKey, from, today, view);
		} else if (app_name == null) {
			// just deployment
			eventsVolumeRequest = buildEventsVolumeRequestDeploymentName(envIdKey, from, today, view, dep_name);
		} else if (dep_name == null) {
			// just app name
			eventsVolumeRequest = buildEventsVolumeRequestApplicationName(envIdKey, from, today, view, app_name);
		} else {
			// both app and deployment
			eventsVolumeRequest = buildEventsVolumeRequestAppAndDepName(envIdKey, from, today, view, dep_name,
					app_name);
		}

		Response<EventsResult> eventsResponse = apiClient.get(eventsVolumeRequest);

		// prepare the map values, to set the values of the Measures
		if (eventsResponse.data.events == null) {
			throw new IllegalStateException("Failed getting events.");
		}else if(eventsResponse.isBadResponse()){
			throw new IllegalStateException("Bad API Response " + appHost + " " + dep_name + " " + app_name);
		}
		CountEvents countEvents = new CountEvents(eventsResponse.data);
		exceptions = countEvents.countAllEventTypes();
		HashMap<String, HashMap<String, Integer>> classErrorCounts = countEvents.countClassErrors();
		//setContexts(context);
		setFileContexts(context, classErrorCounts, countEvents);
	}

	public EventsVolumeRequest buildEventsVolumeRequest(String envIdKey, Instant from, Instant today,
			SummarizedView view) {
		return EventsVolumeRequest.newBuilder().setServiceId(envIdKey.toUpperCase()).setFrom(from.toString())
				.setTo(today.toString()).setViewId(view.id).setVolumeType(VolumeType.all).build();
	}

	public EventsVolumeRequest buildEventsVolumeRequestDeploymentName(String envIdKey, Instant from, Instant today,
			SummarizedView view, String deploymentName) {
		return EventsVolumeRequest.newBuilder().setServiceId(envIdKey.toUpperCase()).setFrom(from.toString())
				.setTo(today.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addDeployment(deploymentName)
				.build();
	}

	public EventsVolumeRequest buildEventsVolumeRequestApplicationName(String envIdKey, Instant from, Instant today,
			SummarizedView view, String appName) {
		return EventsVolumeRequest.newBuilder().setServiceId(envIdKey.toUpperCase()).setFrom(from.toString())
				.setTo(today.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addApp(appName).build();
	}

	public EventsVolumeRequest buildEventsVolumeRequestAppAndDepName(String envIdKey, Instant from, Instant today,
			SummarizedView view, String depName, String appName) {
		return EventsVolumeRequest.newBuilder().setServiceId(envIdKey.toUpperCase()).setFrom(from.toString())
				.setTo(today.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addApp(appName)
				.addDeployment(depName).build();
	}

	public void setFileContexts(SensorContext context, HashMap<String, HashMap<String, Integer>> classErrorCountMap,
			CountEvents eventCount) {
		FileSystem fs = context.fileSystem();
		// only "main" files, but not "tests"
		Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
		for (InputFile file : files) {
			String shortEnedName = file.filename().substring(0, file.filename().indexOf('.'));
			if (classErrorCountMap.containsKey(shortEnedName)) {
				LOGGER.info("Class has errors " + shortEnedName);
				//looping over the nested HashMaps keyset(the type of exceptions OO has in the api)
				for (String key : classErrorCountMap.get(shortEnedName).keySet()) {
					// translation: forMetric- translates OO event type to the metric value. on the
					// file currently at, and withValue uses the shortened fileName to find the
					// error currently writing and count for it
					LOGGER.info("->>>>>>>>>>>>>> shortName key: "+ key + " File: " + file.filename() + ", " + "ShortName: " + shortEnedName); 
					context.<Integer>newMeasure().forMetric(eventCount.typeToMetricMap.get(key)).on(file)
							.withValue(classErrorCountMap.get(shortEnedName).get(key)).save();
				}
			}
		}
	}
}
