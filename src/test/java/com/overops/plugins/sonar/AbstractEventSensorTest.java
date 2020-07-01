package com.overops.plugins.sonar;

import com.overops.plugins.sonar.impl.TestSensorContext;
import com.overops.plugins.sonar.model.Event;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.overops.plugins.sonar.config.Properties.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class AbstractEventSensorTest {

	private EventSensor tester;

	public abstract EventSensor defineSensor();


	@BeforeEach
	public void beforeEach(){
		tester = defineSensor();
	}

	@Test
	void execute() {
		SensorContext context = new TestSensorContext();

		assertDoesNotThrow(() -> tester.execute(context));
	}

	@Test
	void executeInvalidConfig() {
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(API_URL);
		context.config.config.put(API_URL, "");

		assertDoesNotThrow(() -> tester.execute(context));
	}

	@Test
	void validateConfigAppUrl() {
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(APP_URL);
		context.config.config.put(APP_URL, "");

		tester.initConfig(context);

		assertDoesNotThrow(() -> tester.validateConfig());
	}

	@Test
	void validateConfigApiKey() {
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(API_KEY);
		context.config.config.put(API_KEY, "");

		tester.initConfig(context);

		assertThrows(IllegalArgumentException.class, () -> tester.validateConfig());
	}

	@Test
	void validateConfigEnvId() {
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(ENVIRONMENT_ID);
		context.config.config.put(ENVIRONMENT_ID, "");

		tester.initConfig(context);

		assertThrows(IllegalArgumentException.class, () -> tester.validateConfig());
	}

	@Test
	void validateConfigDepName() {
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove(DEPLOYMENT_NAME);
		context.config.config.put(DEPLOYMENT_NAME, "");

		tester.initConfig(context);

		assertThrows(IllegalArgumentException.class, () -> tester.validateConfig());
	}

	@Test
	void validateConfigLogin() {
		TestSensorContext context = new TestSensorContext();

		context.config.config.remove("sonar.login");
		context.config.config.put("sonar.login", "");

		tester.initConfig(context);

		assertThrows(IllegalArgumentException.class, () -> tester.validateConfig());
	}

	@Test
	void addIssuesAndMetrics() {
		SensorContext context = new TestSensorContext();

		tester.initConfig(context);

		Map.Entry<String, ArrayList<Event>> fileEvent = fileEvent(tester);

		assertDoesNotThrow(() -> {
			tester.addIssuesAndMetrics(context, fileEvent);
		});
	}

	@Test
	void addIssuesAndMetricsNullSourceFile() {
		TestSensorContext context = new TestSensorContext();

		context.fileSystem.sourceFile = null;

		tester.initConfig(context);

		Map.Entry<String, ArrayList<Event>> fileEvent = fileEvent(tester);

		assertDoesNotThrow(() -> {
			tester.addIssuesAndMetrics(context, fileEvent);
		});
	}

	@Test
	void saveFileEventsEmpty() {
		TestSensorContext context = new TestSensorContext();

		tester.initConfig(context);

		HashMap<String, ArrayList<Event>> fileEvents = new HashMap<>();

		assertDoesNotThrow(() -> {
			tester.saveFileEvents(fileEvents);
		});
	}

	private Map.Entry<String, ArrayList<Event>> fileEvent(EventSensor tester) {
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
		list.add("source_file_path");
		list.add(10d);

		ArrayList<List<Object>> listOfList = new ArrayList<List<Object>>(1);
		listOfList.add(list);

		events.values = listOfList;

		HashMap<String, ArrayList<Event>> fileEvents = tester.mapFileEvents(events);

		Map.Entry<String, ArrayList<Event>> fileEvent = fileEvents.entrySet().iterator().next();

		return fileEvent;
	}
}