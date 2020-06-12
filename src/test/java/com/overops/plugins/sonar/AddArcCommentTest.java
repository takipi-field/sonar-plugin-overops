package com.overops.plugins.sonar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.overops.plugins.sonar.model.Event;
import com.overops.plugins.sonar.model.EventsJson;
import com.overops.plugins.sonar.model.IssueComment;
import com.takipi.api.client.functions.output.Series;
import com.takipi.api.client.functions.output.SeriesRow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;

public class AddArcCommentTest {

	@BeforeEach
	public void beforeAll(){
		System.setProperty("overops.pauseForTheCause","0");
	}

	@Test
	public void execute() throws IOException {
		EventsJson eventsJson = new EventsJson();
		eventsJson.setRule("test rule");
		eventsJson.setComponentKey("test:key");

		List<IssueComment> issueList = new ArrayList<>(1);
		eventsJson.setIssues(issueList);

		Series<SeriesRow> events = new Series<>();

		ArrayList<Object> list = new ArrayList<>(8);
		list.add("0"); // id
		// stack_frames
		list.add(
				"[{prettified_name:\"test\",class_name:\"test\",method_name:\"test\",method_desc:\"test\",original_line_number:0,method_position:0,in_filter:false}]");
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

		Event event = new Event(events, 0, "", "", "");

		// save for later
		IssueComment issueComment = new IssueComment(event);
		eventsJson.getIssues().add(issueComment);

		EventDataStore.instance().setData(Arrays.asList(eventsJson));

		AddArcComment tester = new AddArcComment();
		PostJobContext context = new TestPostJobContextImpl();

		assertDoesNotThrow(() -> tester.execute(context));
	}

	@Test
	public void executeEmpty() throws IOException {
		EventDataStore.instance().setData(new ArrayList<EventsJson>(0));

		AddArcComment tester = new AddArcComment();
		PostJobContext context = null;

		assertDoesNotThrow(() -> tester.execute(context));
	}

	@Test
	public void executeException() throws IOException {
		EventDataStore.instance().setData(null);

		AddArcComment tester = new AddArcComment();
		PostJobContext context = null;

		assertDoesNotThrow(() -> tester.execute(context));
	}

	@Test
	public void setHttpContext() throws URISyntaxException {
		EventDataStore.instance().setData(null);
		AddArcComment tester = new AddArcComment();
		PostJobContext context = new TestPostJobContextImpl();
		assertDoesNotThrow(() -> tester.setHttpContext(context));
	}

	private class TestPostJobContextImpl implements PostJobContext {

		@Override
		public Settings settings() {
			return null;
		}

		@Override
		public Configuration config() {
			Configuration config = new TestConfigurationImpl();
			return config;
		}

		@Override
		public AnalysisMode analysisMode() {
			return null;
		}

		@Override
		public Iterable<PostJobIssue> issues() {
			return null;
		}

		@Override
		public Iterable<PostJobIssue> resolvedIssues() {
			return null;
		}

	}

	private class TestConfigurationImpl implements Configuration {

		@Override
		public Optional<String> get(String key) {

			if (key.equals("sonar.login")) return Optional.of("test");
			if (key.equals("sonar.password")) return Optional.of("test");
			if (key.equals("sonar.host.url")) return Optional.of("http://localhost:9000");

			return Optional.ofNullable(null);
		}

		@Override
		public boolean hasKey(String key) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String[] getStringArray(String key) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
