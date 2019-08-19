package com.overops.plugins.sonar.measures;

import java.awt.Event;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.Highlighter.Highlight;

import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventSnapshotRequest;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventSnapshotResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient;
import com.takipi.api.core.url.UrlClient.Response;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

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
		String dep_name = config.get(OverOpsProperties.DEP_NAME).orElse(this.setDeploymentName());
		String app_name = config.get(OverOpsProperties.APP_NAME).orElse(null);
		LOGGER.info("verison number + : " + dep_name);
		
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
		} else if (eventsResponse.isBadResponse()) {
			throw new IllegalStateException("Bad API Response " + appHost + " " + dep_name + " " + app_name);
		}
		eventList = eventsResponse.data;
		CountEvents countEvents = new CountEvents(eventsResponse.data);
		exceptions = countEvents.countAllEventTypes();
		LOGGER.info("made it past exceptions");
		HashMap<String, HashMap<String, Integer>> classErrorCounts = countEvents.countClassErrors();
		LOGGER.info("made it past classErrorCounts Map");
		setFileContexts(context, classErrorCounts, countEvents);
		LOGGER.info("Set the file contexts");
//		getTinyLinkForEvent(eventId, envIdKey, apiClient, app_name);
//		setMethodHighlights(context, countEvents);
	}
	/* This method pulls in the tiny link based on the eventId passed
	*/
	public String getTinyLinkForEvent(String eventId, String envId, RemoteApiClient apiClient, String app){
		EventSnapshotRequest request = EventSnapshotRequest.newBuilder().addApp(app).setEventId(eventId)
				.setServiceId(envId).build();
		Response<EventSnapshotResult> tinyLink = apiClient.get(request);
		return tinyLink.data.link;
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
	/*
	*Major Problem 1: what is an offset exactly- seems to be a position inside of the file that cannot be highlighted twice
	* Major Problem 2: there can only be one highlight per file...
	*/
	public void setMethodHighlights(SensorContext context, CountEvents eventCount) {
		eventCount.classToMethodBuilder();
		FileSystem fs = context.fileSystem();
		Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
		// loop through all MAIN files

		for (InputFile file : files) {
			NewHighlighting highlight = context.newHighlighting().onFile(file);
			String shortenedName = file.filename().substring(0, file.filename().indexOf('.'));
			// if the shortnedName of the file is inside of the classError map
			if (eventCount.classNameToMethodNameMap.containsKey(shortenedName)) {				
				// loop through all the methods that threw errors inside of this file
				//highlight.onFile(file).highlight(range, TypeOfText.STRING);
				LOGGER.info("Found the classname in the map: " + shortenedName);
				BufferedReader reader;
				try {
					reader = new BufferedReader(new InputStreamReader(file.inputStream()));
				} catch (IOException e1) {
					e1.printStackTrace();
					LOGGER.info("Buffered reader in OOSensor.java messed up the Buffered reader aint working");
					break;
				}
				for (String methodName : eventCount.classNameToMethodNameMap.get(shortenedName)) {
					try {
						int counter = 1; //default is line 1 i think
						while(reader.ready()){
							String line = reader.readLine();
							if(line.contains(" " + methodName + "(")){
								//this line is the one that should be adding the highlights but its not ugh
								//I am not sure what offset is exactly but that 2nd 0 needs to change to the end of the statement but it throws an error generally when it is not 0 meaning nothing to highlight
								highlight.highlight(counter, 0, counter+1, 0, TypeOfText.STRING);
								LOGGER.info("line count : "+ counter);
								LOGGER.info(line);
								break;
							}
							++counter;
						}
						counter=0;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try{
					reader.close();
				}catch(IOException e1){
					e1.printStackTrace();
				}
			}
		}
	}

	public void setFileContexts(SensorContext context, HashMap<String, HashMap<String, Integer>> classErrorCountMap,
			CountEvents eventCount) {
		int count = 0;
		FileSystem fs = context.fileSystem();
		// only "main" files, but not "tests"
		Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
		for (InputFile file : files) {
			String shortEnedName = file.filename().substring(0, file.filename().indexOf('.'));
			if (classErrorCountMap.containsKey(shortEnedName)) {
				LOGGER.info("Class has errors " + shortEnedName);
				// looping over the nested HashMaps keyset(the type of exceptions OO has in the
				// api)
				for (String key : classErrorCountMap.get(shortEnedName).keySet()) {
					// translation: forMetric- translates OO event type to the metric value. on the
					// file currently at, and withValue uses the shortened fileName to find the
					// error currently writing and count for it
					context.<Integer>newMeasure().forMetric(eventCount.typeToMetricMap.get(key)).on(file)
							.withValue(classErrorCountMap.get(shortEnedName).get(key)).save();
				}
				// this is where the new highlights need to be added in but I am not sure how I would do it considering they have this thing called a page offset and require line number
			}
			++count;
		}
	}

	public String setDeploymentName(){
		String ret = "";
		MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			Model model = reader.read(new FileReader("pom.xml"));
			ret = model.getVersion();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
}
