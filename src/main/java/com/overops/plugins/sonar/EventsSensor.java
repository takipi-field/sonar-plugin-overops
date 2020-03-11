package com.overops.plugins.sonar;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.overops.plugins.sonar.config.JavaRulesDefinition;
import com.overops.plugins.sonar.model.Event;
import com.overops.plugins.sonar.model.EventsJson;
import com.overops.plugins.sonar.model.IssueComment;
import com.overops.plugins.sonar.model.JsonStore;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.functions.input.EventsInput;
import com.takipi.api.client.functions.output.QueryResult;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
import com.takipi.api.client.util.regression.RegressionUtil;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.common.util.Pair;
import com.takipi.common.util.TimeUtil;

import org.apache.commons.lang.StringUtils;

import org.joda.time.DateTime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.overops.plugins.sonar.config.Properties.*;
import static com.overops.plugins.sonar.config.JavaRulesDefinition.*;
import static com.overops.plugins.sonar.config.OverOpsMetrics.*;
import static com.overops.plugins.sonar.model.JsonStore.STORE_FILE;

public class EventsSensor implements Sensor {

	private static final Logger LOGGER = Loggers.get(EventsSensor.class);

	private String apiUrl;
	private String appUrl;
	private String apiKey;
	private String envId;
	private String appName;
	private String depName;
	private String criticalExceptionTypes;
	private String ignoreTypes;
	private String login;

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("OverOps Event Issues Sensor");
		descriptor.onlyOnLanguage("java");
		descriptor.createIssuesForRuleRepositories(JavaRulesDefinition.REPOSITORY);
	}

	@Override
	public void execute(SensorContext context) {

		initConfig(context);

		try {
			validateConfig();
		} catch (IllegalArgumentException ex) {
			LOGGER.warn(ex.getMessage());
			return;
		}

		try {
			// construct overops api client
			RemoteApiClient apiClient = (RemoteApiClient) RemoteApiClient
				.newBuilder().setApiKey(apiKey).setHostname(apiUrl).build();

			Pair<DateTime, DateTime> depTimes = RegressionUtil
				.getDeploymentsActiveWindow(apiClient, envId, Arrays.asList(depName.split(",")));

			// stop if deployment not found
			if (depTimes == null || depTimes.getFirst() == null) {
				LOGGER.error("Deployment " + depName + " not found.");
				return;
			}

			LOGGER.debug("timefilter: " + TimeUtil.getTimeFilter(depTimes));

			EventsInput eventsInput = new EventsInput();
			eventsInput.fields = Event.FIELDS; // defines response columns
			eventsInput.timeFilter = TimeUtil.getTimeFilter(depTimes);
			eventsInput.environments = envId.toUpperCase();
			eventsInput.applications = appName;
			eventsInput.deployments = depName;
			eventsInput.servers = "All";
			eventsInput.types = "All"; 

			Response<QueryResult> response = apiClient.get(eventsInput);

			if (response.isBadResponse()) {
				LOGGER.error("OverOps encountered an error retrieving events");
				return;
			}

			Collection<Series<SeriesRow>> series = response.data.getSeries();

			// this shouldn't happen, but stop here if it does
			if (!series.iterator().hasNext()) {
				return;
			}

			Series<SeriesRow> events = series.iterator().next();

			// loop through all the events, mapping them by source file (there can be multiple issues per file)
			HashMap<String, ArrayList<Event>> fileEvents = mapFileEvents(events);

			// stop here if there are no events
			if (fileEvents.size() == 0) {
				LOGGER.info("No OverOps events found.");
				return;
			}

			// save for later
			JsonStore jsonStore = new JsonStore();
			jsonStore.setEventsJson(new ArrayList<EventsJson>(fileEvents.size()));

			for (Map.Entry<String, ArrayList<Event>> fileEvent : fileEvents.entrySet()) {
				addIssuesAndMetrics(context, fileEvent, jsonStore);
			}

			// save to disk
			String jsonified = new Gson().toJson(jsonStore);
			try (FileWriter writer = new FileWriter(STORE_FILE)) {
				writer.write(jsonified);
			}

		} catch (Exception ex) {
			LOGGER.error("OverOps sensor encountered an error.");
			LOGGER.error(ex.getMessage());

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);

			ex.printStackTrace(pw);

			LOGGER.error(sw.toString()); // stack trace as a string
		}
	}

	HashMap<String, ArrayList<Event>> mapFileEvents(Series<SeriesRow> events) {
		HashMap<String, ArrayList<Event>> fileEvents = new HashMap<>();
		for (int i = 0; i < events.size(); i++) {
			try {
				Event event = new Event(events, i, depName, criticalExceptionTypes, appUrl);

				// ignore specific types
				if (ignoreTypes.contains(event.getType())) {
					LOGGER.debug("skipping " + event.getMessage());
					continue;
				}

				fileEvents.putIfAbsent(event.getKey(), new ArrayList<Event>());
				fileEvents.get(event.getKey()).add(event);

			} catch (IllegalArgumentException ex) {
				LOGGER.warn(ex.getMessage()); // when unable to parse stack_frames
			}
		}
		return fileEvents;
	}

	void initConfig(SensorContext context) {
		apiUrl = context.config().get(API_URL).orElse(DEFAULT_API_URL);
		appUrl = context.config().get(APP_URL).orElse(DEFAULT_APP_URL);
		apiKey = context.config().get(API_KEY).orElse(null);
		envId = context.config().get(ENVIRONMENT_ID).orElse(null);

		appName = context.config().get(APPLICATION_NAME).orElse(DEFAULT_APPLICATION_NAME);

		// overops.deployment.name > sonar.buildString
		depName = context.config().get(DEPLOYMENT_NAME)
				.orElse(context.config().get("sonar.buildString").orElse(null));

		criticalExceptionTypes = context.config().get(CRITICAL_EXCEPTION_TYPES)
				.orElse(DEFAULT_CRITICAL_EXCEPTION_TYPES);

		ignoreTypes = context.config().get(IGNORE_EVENT_TYPES).orElse(DEFAULT_IGNORE_EVENT_TYPES);

		login = context.config().get("sonar.login").orElse(null);
	}

	void validateConfig() {

		if (StringUtils.isBlank(apiUrl)) {
			throw new IllegalArgumentException("OverOps API URL is required.");
		}

		// default to API URL if app URL is missing
		if (StringUtils.isBlank(appUrl)) {
			appUrl = apiUrl;
		}

		if (StringUtils.isBlank(apiKey)) {
			throw new IllegalArgumentException("OverOps API Token is required.");
		}

		if (StringUtils.isBlank(envId)) {
			throw new IllegalArgumentException("OverOps Environment ID is required.");
		}

		// dep name needed to calculate timeframe
		if (StringUtils.isBlank(depName)) {
			throw new IllegalArgumentException("OverOps Deployment Name is required.");
		}

		// sonar login is needed to add post job comments
		if (StringUtils.isBlank(login)) {
			throw new IllegalArgumentException("Sonar login token or username and password are required.");
		}

		LOGGER.debug(API_URL + ": " + apiUrl);
		LOGGER.debug(APP_URL + ": " + appUrl);
		LOGGER.debug(API_KEY + ": " + apiKey.substring(0, 5) + "***************");
		LOGGER.debug(ENVIRONMENT_ID + ": " + envId);

		LOGGER.debug(APPLICATION_NAME + ": " + appName);
		LOGGER.debug(DEPLOYMENT_NAME + ": " + depName);

		LOGGER.debug(CRITICAL_EXCEPTION_TYPES + ": " + criticalExceptionTypes);

	}

	void addIssuesAndMetrics(SensorContext context, Map.Entry<String, ArrayList<Event>> fileEvent, JsonStore jsonStore) {
		// add issues and measures to each file
		FileSystem fs = context.fileSystem();

		String filePath = fileEvent.getKey();
		ArrayList<Event> eventList = fileEvent.getValue();

		// get file matching this filePath (e.g. **/com/example/path/ClassName.java)
		InputFile sourceFile = fs.inputFile(
			fs.predicates().and(
				fs.predicates().matchesPathPattern(filePath),
				fs.predicates().hasLanguage("java")
			)
		);

		if (sourceFile == null) {
			return;
		}

		LOGGER.debug("src: " + sourceFile);

		Integer newCount = 0;
		Integer criticalCount = 0;
		Integer resurfacedCount = 0;

		EventsJson eventsJson = new EventsJson();
		eventsJson.setRule(EVENT_RULE.toString());
		eventsJson.setComponentKey(context.project().key() + ":" + sourceFile);

		List<IssueComment> issueList = new ArrayList<>(eventList.size());
		eventsJson.setIssues(issueList);

		// add issues
		for (Event event : eventList) {
			LOGGER.debug("creating new issue for event: " + event.toString());
			NewIssue newIssue = context.newIssue().forRule(EVENT_RULE).gap(ARBITRARY_GAP);

			NewIssueLocation primaryLocation = newIssue.newLocation()
				.on(sourceFile)
				.at(sourceFile.selectLine(event.getLocation().original_line_number)) // int line number
				// message must not be greater than MESSAGE_MAX_SIZE
				.message(StringUtils.abbreviate(event.getMessage(), NewIssueLocation.MESSAGE_MAX_SIZE));
			newIssue.at(primaryLocation);
			newIssue.save();

			// count measures
			if (event.isNew()) newCount++;
			if (event.isCritical()) criticalCount++;
			if (event.isResurfaced()) resurfacedCount++;

			// save for later
			IssueComment issueComment = new IssueComment(event);
			eventsJson.getIssues().add(issueComment);
		}

		// add measures
		context.<Integer>newMeasure()
			.forMetric(NEW)
			.on(sourceFile)
			.withValue(newCount)
			.save();

		context.<Integer>newMeasure()
			.forMetric(CRITICAL)
			.on(sourceFile)
			.withValue(criticalCount)
			.save();

		context.<Integer>newMeasure()
			.forMetric(RESURFACED)
			.on(sourceFile)
			.withValue(resurfacedCount)
			.save();

		context.<Integer>newMeasure()
			.forMetric(UNIQUE)
			.on(sourceFile)
			.withValue(eventList.size())
			.save();

		// save to temporary file to add comments in post job step
		jsonStore.getEventsJson().add(eventsJson);
	}
}
