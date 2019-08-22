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
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/*
*This class is responsible for calling anythign relating to pulling OverOps data and aggregating some counts
*this classes lifecycle is heavily limited so anything brought in that is not saved will be gone before Sonar begins to display data
*currently saving onto the context.module(). On a file is preferred because it will persist but as of now we dont have line numbers.
*/
public class OOSensor implements Sensor {

	private static final Logger LOGGER = Loggers.get(OOSensor.class);
	private static String ENVIDKEY;
	private static String APPHOST;
	private static String APIKEY;
	private static String DEPNAME;
	private static String APPNAME;
	private static Instant TODAY = Instant.now();
	private static Instant FROM;
	public static RemoteApiClient APICLIENT;

	public EventsResult eventList;
	public EventsVolumeRequest eventsVolumeRequest;
	// the event type labels
	public final String CAUGHTEXCEPTION = "Caught Exception";
	public final String SWALLOWEDEXCEPTION = "Swallowed Exception";
	public final String UNCAUGHTEXCEPTION = "Uncaught Exception";
	public final String LOGGEDERROR = "Logged Error";
	public final String CUSTOMEVENT = "Custom Event";
	public final String HTTPERROR = "HTTP Error";
	public final String CRITICALEXCEPTION = "Critical Exception";

	public HashMap<String, Integer> exceptions;

	// This is displaying in logs(maven output) so when this sensor runs this gets
	// displayed
	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name(
				"OverOps sensor calling the Summarized View API with customer configuration and setting up the default Measures");
	}

	@Override
	public void execute(SensorContext context) {
		Configuration config = context.config();
		ENVIDKEY = config.get(OverOpsProperties.ooENVID).orElse(null);
		APPHOST = config.get(OverOpsProperties.ooURL).orElse("https://api.overops.com");
		APIKEY = config.get(OverOpsProperties.apiKEY).orElse(null);
		DEPNAME = config.get(OverOpsProperties.depNAME).orElse(setDeploymentName());
		APPNAME = config.get(OverOpsProperties.appNAME).orElse(null);
		LOGGER.info("verison number : " + DEPNAME);
		LOGGER.info("app name " + APPNAME);

		if (APIKEY == null || ENVIDKEY == null) {

			return;
		}

		APICLIENT = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(APIKEY).setHostname(APPHOST).build();
		//brings in all events based on time range
		SummarizedView view = ViewUtil.getServiceViewByName(APICLIENT, ENVIDKEY, "All Events");
		long days = config.getLong(OverOpsProperties.DAYS).orElse(1l);// default 1 day
		FROM = TODAY.minus(days, ChronoUnit.DAYS);

		// seperate cases if the user does not provide some information
		if (DEPNAME == null && APPNAME == null) {
			eventsVolumeRequest = buildEventsVolumeRequest(view);
		} else if (APPNAME == null) {
			eventsVolumeRequest = buildEventsVolumeRequestDeploymentName(view);
		} else if (DEPNAME == null) {
			eventsVolumeRequest = buildEventsVolumeRequestApplicationName(view);
		} else {
			eventsVolumeRequest = buildEventsVolumeRequestAppAndDepName(view);
		}

		Response<EventsResult> eventsResponse = APICLIENT.get(eventsVolumeRequest);

		// prepare the map values, to set the values of the Measures
		if (eventsResponse.data.events == null) {
			throw new IllegalStateException(
					"Failed getting OverOps events, please check Application Name and Deployment Name.");
		} else if (eventsResponse.isBadResponse()) {
			throw new IllegalStateException("Bad API Response " + APPHOST + " " + DEPNAME + " " + APPNAME);
		}
		eventList = eventsResponse.data;
		CountEvents countEvents = new CountEvents(eventsResponse.data);
		exceptions = countEvents.countAllEventTypes();
		LOGGER.info("made it past exceptions");
		// classname to exception type to count
		HashMap<String, HashMap<String, Integer>> classErrorCounts = countEvents.countClassErrors();
		LOGGER.info("made it past classErrorCounts Map");
		setFileContexts(context, classErrorCounts, countEvents);
		LOGGER.info("Set the file contexts");
		// setMethodHighlights(context, countEvents); this does not work
		createExternalIssuePerEvent(context);
	}

	public String shortenEventName(EventResult event) {
		String shortEnedName;
		if (event.error_location.class_name.contains(".")) {
			int lastPeriod = event.error_location.class_name.lastIndexOf('.');
			shortEnedName = event.error_location.class_name.substring(lastPeriod + 1,
					event.error_location.class_name.length());
		} else {
			shortEnedName = event.error_location.class_name;
		}
		return shortEnedName;
	}

	public String shortenFileName(InputFile file) {
		return file.filename().substring(0, file.filename().lastIndexOf("."));
	}

	public void createExternalIssuePerEvent(SensorContext context) {
		FileSystem fs = context.fileSystem();
		Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
		for (InputFile file : files) {
			int lineCount = 1;
			String shortFileName = shortenFileName(file);
			for (EventResult event : eventList.events) {
				String shortClassName = shortenEventName(event);
				LOGGER.info("Full file name: " + file.filename());
				LOGGER.info("Full class name: " + event.error_location.class_name);
				LOGGER.info("Short file name: " + shortFileName);
				LOGGER.info("Short class name: " + shortClassName);
				if (shortClassName.equals(shortFileName)) {
					createIssue(context, file, event, lineCount);
					lineCount++;
				}
			}
		}
	}

	/*
	 * This method pulls in the tiny link based on the eventId passed
	 */
	private String getTinyLinkForEvent(String eventId) {
		LOGGER.info("entering call for tinylinks");
		EventSnapshotRequest request = EventSnapshotRequest.newBuilder().setEventId(eventId).setServiceId(ENVIDKEY)
				.setFrom(TODAY.minus(2l, ChronoUnit.DAYS).toString()).setTo(TODAY.toString()).addApp(APPNAME)
				.addDeployment(DEPNAME).build();
		Response<EventSnapshotResult> tinyLink = APICLIENT.get(request);
		LOGGER.info(tinyLink.data.link);
		return tinyLink.data.link;
	}

	public void createIssue(SensorContext context, InputFile file, EventResult event, int lineNum) {
		String tinyLink = getTinyLinkForEvent(event.id);
		NewExternalIssue newIssue = context.newExternalIssue();
		newIssue.engineId("OverOps_Scan").ruleId(event.type + event.id)
				.at(newIssue.newLocation().on(file).at(file.selectLine(lineNum))
						.message(" link to ARC screen " + event.error_location.class_name + tinyLink))
				.severity(Severity.MAJOR).remediationEffortMinutes(101l).type(RuleType.BUG).save();
	}

	private EventsVolumeRequest buildEventsVolumeRequest(SummarizedView view) {
		return EventsVolumeRequest.newBuilder().setServiceId(ENVIDKEY.toUpperCase()).setFrom(FROM.toString())
				.setTo(TODAY.toString()).setViewId(view.id).setVolumeType(VolumeType.all).build();
	}

	private EventsVolumeRequest buildEventsVolumeRequestDeploymentName(SummarizedView view) {
		return EventsVolumeRequest.newBuilder().setServiceId(ENVIDKEY.toUpperCase()).setFrom(FROM.toString())
				.setTo(TODAY.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addDeployment(DEPNAME)
				.build();
	}

	private EventsVolumeRequest buildEventsVolumeRequestApplicationName(SummarizedView view) {
		return EventsVolumeRequest.newBuilder().setServiceId(ENVIDKEY.toUpperCase()).setFrom(FROM.toString())
				.setTo(TODAY.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addApp(APPNAME).build();
	}

	private EventsVolumeRequest buildEventsVolumeRequestAppAndDepName(SummarizedView view) {
		return EventsVolumeRequest.newBuilder().setServiceId(ENVIDKEY.toUpperCase()).setFrom(FROM.toString())
				.setTo(TODAY.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addApp(APPNAME)
				.addDeployment(DEPNAME).build();
	}

	/*
	 * Major Problem 1: what is an offset exactly- seems to be a position inside of
	 * the file that cannot be highlighted twice Major Problem 2: there can only be
	 * one highlight per file...
	 */
	/*
	 * public void setMethodHighlights(SensorContext context, CountEvents
	 * eventCount) { eventCount.classToMethodBuilder(); FileSystem fs =
	 * context.fileSystem(); Iterable<InputFile> files =
	 * fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN)); // loop through
	 * all MAIN files
	 * 
	 * for (InputFile file : files) { NewHighlighting highlight =
	 * context.newHighlighting().onFile(file); String shortenedName =
	 * file.filename().substring(0, file.filename().indexOf('.')); // if the
	 * shortnedName of the file is inside of the classError map if
	 * (eventCount.classNameToMethodNameMap.containsKey(shortenedName)) { // loop
	 * through all the methods that threw errors inside of this file //
	 * highlight.onFile(file).highlight(range, TypeOfText.STRING);
	 * LOGGER.info("Found the classname in the map: " + shortenedName);
	 * BufferedReader reader; try { reader = new BufferedReader(new
	 * InputStreamReader(file.inputStream())); } catch (IOException e1) {
	 * e1.printStackTrace(); LOGGER.
	 * info("Buffered reader in OOSensor.java messed up the Buffered reader aint working"
	 * ); break; } for (String methodName :
	 * eventCount.classNameToMethodNameMap.get(shortenedName)) { try { int lineCount
	 * = 1; // default is line 1 i think while (reader.ready()) { String line =
	 * reader.readLine(); if(line.contains(" " + methodName + "(")){ // this line is
	 * the one that should be adding the highlights but its not ugh // I am not sure
	 * what offset is exactly but that 2nd 0 needs to change to the // end of the
	 * statement but it throws an error generally when it is not 0 // meaning
	 * nothing to highlight highlight.highlight(file.selectLine(lineCount),
	 * TypeOfText.CONSTANT); LOGGER.info("line count : " + lineCount);
	 * LOGGER.info(line); break; } ++lineCount; } lineCount = 0; } catch
	 * (IOException e) { e.printStackTrace(); } break; } highlight.save(); try {
	 * reader.close(); } catch (IOException e1) { e1.printStackTrace(); } } } }
	 */

	public void setFileContexts(SensorContext context, HashMap<String, HashMap<String, Integer>> classErrorCountMap,
			CountEvents eventCount) {
		FileSystem fs = context.fileSystem();
		// only "main" files, but not "tests"
		Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
		for (InputFile file : files) {
			String shortEnedName = file.filename().substring(0, file.filename().indexOf('.'));
			if (classErrorCountMap.containsKey(shortEnedName)) {
				LOGGER.info("Class has errors " + shortEnedName);
				// looping over the nested HashMaps keyset(the type of exceptions OO has in the
				// looping over methods below
				for (String exceptionType : classErrorCountMap.get(shortEnedName).keySet()) {
					// translation: forMetric- translates OO event type to the metric value. on the
					// file currently at, and withValue uses the shortened fileName to find the
					// error currently writing and count for it
					context.<Integer>newMeasure().forMetric(eventCount.typeToMetricMap.get(exceptionType)).on(file)
							.withValue(classErrorCountMap.get(shortEnedName).get(exceptionType)).save();
				}
				// this is where the new highlights need to be added in but I am not sure how I
				// would do it considering they have this thing called a page offset and require
				// line number
			}
		}
	}
// BNY uses the <version> tag in the pom.xml
	public String setDeploymentName() {
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
