package com.overops.plugins.sonar;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

import static com.overops.plugins.sonar.Properties.*;
import static com.overops.plugins.sonar.JavaRulesDefinition.*;
import static com.overops.plugins.sonar.OverOpsMetrics.*;

public class EventsSensor implements Sensor {

	private static final Logger LOGGER = Loggers.get(EventsSensor.class);

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("OverOps Event Issues Sensor");
		descriptor.onlyOnLanguage("java");
		descriptor.createIssuesForRuleRepositories(JavaRulesDefinition.REPOSITORY);
	}

	@Override
	public void execute(SensorContext context) {
		String apiUrl = context.config().get(API_URL).orElse(DEFAULT_API_URL);
		String apiKey = context.config().get(API_KEY).orElse(null);
		String envId = context.config().get(ENVIRONMENT_ID).orElse(null);

		String appName = context.config().get(APPLICATION_NAME).orElse(DEFAULT_APPLICATION_NAME);

		// overops.deployment.name > sonar.buildString
		String depName = context.config().get(DEPLOYMENT_NAME)
				.orElse(context.config().get("sonar.buildString").orElse(null));

		String criticalExceptionTypes = context.config().get(CRITICAL_EXCEPTION_TYPES)
				.orElse(DEFAULT_CRITICAL_EXCEPTION_TYPES);

		LOGGER.debug(API_URL + ": " + apiUrl);
		LOGGER.debug(API_KEY + ": " + apiKey.substring(0, 5) + "***************");
		LOGGER.debug(ENVIRONMENT_ID + ": " + envId);

		LOGGER.debug(APPLICATION_NAME + ": " + appName);
		LOGGER.debug(DEPLOYMENT_NAME + ": " + depName);

		LOGGER.debug(CRITICAL_EXCEPTION_TYPES + ": " + criticalExceptionTypes);

		// validate config
		if (StringUtils.isBlank(apiUrl)) {
			LOGGER.warn("OverOps API URL is required.");
			return;
		}

		if (StringUtils.isBlank(apiKey)) {
			LOGGER.warn("OverOps API Token is required.");
			return;
		}

		if (StringUtils.isBlank(envId)) {
			LOGGER.warn("OverOps Environment ID is required.");
			return;
		}

		// dep name needed to calculate timeframe
		if (StringUtils.isBlank(depName)) {
			LOGGER.info("OverOps Deployment Name is required.");
			return;
		}

		try {
			// construct overops api client
			RemoteApiClient apiClient = (RemoteApiClient) RemoteApiClient
				.newBuilder().setApiKey(apiKey).setHostname(apiUrl).build();

			Pair<DateTime, DateTime> depTimes = RegressionUtil
				.getDeploymentsActiveWindow(apiClient, envId, Arrays.asList(depName.split(",")));

			LOGGER.debug("timefilter: " + TimeUtil.getTimeFilter(depTimes));

			// stop if deployment not found
			if (depTimes == null || depTimes.getFirst() == null) {
				LOGGER.error("Deployment " + depName + " not found.");
				return;
			}

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

			// this query only returns one series
			if (!series.iterator().hasNext()) {
				LOGGER.info("No OverOps events found.");
				return;
			}

			Series<SeriesRow> events = series.iterator().next();

			LOGGER.info("found " + events.size() + " errors");

			// loop through all the events, mapping them by source file (there can be multiple issues per file)
			HashMap<String, ArrayList<Event>> fileEvents = new HashMap<String, ArrayList<Event>>();
			for (int i = 0; i < events.size(); i++) {
				try {
					Event event = new Event(events, i, depName, criticalExceptionTypes);
					fileEvents.putIfAbsent(event.getKey(), new ArrayList<Event>());
					fileEvents.get(event.getKey()).add(event);
				} catch (IllegalArgumentException ex) {
					LOGGER.warn(ex.getMessage()); // when unable to parse stack_frames
				}
			}

			// add issues and measures to each file
			FileSystem fs = context.fileSystem();

			for (Map.Entry<String, ArrayList<Event>> fileEvent : fileEvents.entrySet()) {
				String filePath = fileEvent.getKey();
				ArrayList<Event> eventList = fileEvent.getValue();

				// get file matching this filePath (e.g. **/com/example/path/ClassName.java)
				InputFile sourceFile = fs.inputFile(
					fs.predicates().and(
						fs.predicates().matchesPathPattern(filePath),
						fs.predicates().hasLanguage("java")
					)
				);

				LOGGER.debug("src: " + sourceFile);

				Integer newCount = 0;
				Integer criticalCount = 0;
				Integer resurfacedCount = 0;

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
}
