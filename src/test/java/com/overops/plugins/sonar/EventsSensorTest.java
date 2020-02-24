package com.overops.plugins.sonar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.overops.plugins.sonar.config.Properties.*;
import static com.overops.plugins.sonar.config.JavaRulesDefinition.EVENT_RULE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

import com.overops.plugins.sonar.model.Event;
import com.overops.plugins.sonar.model.EventsJson;
import com.overops.plugins.sonar.model.IssueComment;
import com.overops.plugins.sonar.model.JsonStore;
import com.overops.plugins.sonar.impl.*;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;

import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.Version;

public class EventsSensorTest {

	@Test
	public void execute() {
		EventsSensor tester = new EventsSensor();
		SensorContext context = new TestSensorContext();

		assertDoesNotThrow(() -> tester.execute(context));
	}

	@Test
	public void executeInvalidConfig() {
		EventsSensor tester = new EventsSensor();
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(API_URL);
		context.config.config.put(API_URL, "");

		assertDoesNotThrow(() -> tester.execute(context));
	}

	@Test
	public void validateConfigAppUrl() {
		EventsSensor tester = new EventsSensor();
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(APP_URL);
		context.config.config.put(APP_URL, "");

		tester.initConfig(context);

		assertDoesNotThrow(() -> tester.validateConfig());
	}

	@Test
	public void validateConfigApiKey() {
		EventsSensor tester = new EventsSensor();
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(API_KEY);
		context.config.config.put(API_KEY, "");

		tester.initConfig(context);

		assertThrows(IllegalArgumentException.class, () -> tester.validateConfig());
	}

	@Test
	public void validateConfigEnvId() {
		EventsSensor tester = new EventsSensor();
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(ENVIRONMENT_ID);
		context.config.config.put(ENVIRONMENT_ID, "");

		tester.initConfig(context);

		assertThrows(IllegalArgumentException.class, () -> tester.validateConfig());
	}

	@Test
	public void validateConfigDepName() {
		EventsSensor tester = new EventsSensor();
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(DEPLOYMENT_NAME);
		context.config.config.put(DEPLOYMENT_NAME, "");

		tester.initConfig(context);

		assertThrows(IllegalArgumentException.class, () -> tester.validateConfig());
	}

	@Test
	public void validateConfigLogin() {
		EventsSensor tester = new EventsSensor();
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove("sonar.login");
		context.config.config.put("sonar.login", "");

		tester.initConfig(context);

		assertThrows(IllegalArgumentException.class, () -> tester.validateConfig());
	}

	@Test
	public void addIssuesAndMetrics() {
		EventsSensor tester = new EventsSensor();
		SensorContext context = new TestSensorContext();

		tester.initConfig(context);

		Map.Entry<String, ArrayList<Event>> fileEvent = fileEvent(tester);

		JsonStore jsonStore = new JsonStore();
		jsonStore.setEventsJson(new ArrayList<EventsJson>(1));

		assertDoesNotThrow(() -> {
			tester.addIssuesAndMetrics(context, fileEvent, jsonStore);
		});
	}

	@Test
	public void addIssuesAndMetricsNullSourceFile() {
		EventsSensor tester = new EventsSensor();
		TestSensorContext context = new TestSensorContext();

		context.fileSystem.sourceFile = null;

		tester.initConfig(context);

		Map.Entry<String, ArrayList<Event>> fileEvent = fileEvent(tester);

		JsonStore jsonStore = new JsonStore();
		jsonStore.setEventsJson(new ArrayList<EventsJson>(1));

		assertDoesNotThrow(() -> {
			tester.addIssuesAndMetrics(context, fileEvent, jsonStore);
		});
	}

	private Map.Entry<String, ArrayList<Event>> fileEvent(EventsSensor tester) {
		Series<SeriesRow> events = new Series<>();

		ArrayList<Object> list = new ArrayList<>(8);
		list.add("0"); // id
		// stack_frames
		list.add(
				"[{prettified_name:\"test\",class_name:\"test\",method_name:\"test\",method_desc:\"test\",original_line_number:1,method_position:0,in_filter:false}]");
		list.add("link");
		list.add("name");
		list.add("type");
		list.add("message");
		list.add("introduced_by");
		list.add("labels");

		ArrayList<List<Object>> listOfList = new ArrayList<List<Object>>(1);
		listOfList.add(list);

		events.values = listOfList;

		HashMap<String, ArrayList<Event>> fileEvents = tester.mapFileEvents(events);

		Map.Entry<String, ArrayList<Event>> fileEvent = fileEvents.entrySet().iterator().next();

		return fileEvent;
	}
}