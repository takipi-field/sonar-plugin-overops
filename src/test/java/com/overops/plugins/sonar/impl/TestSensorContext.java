package com.overops.plugins.sonar.impl;

import java.io.Serializable;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.Version;

public class TestSensorContext implements SensorContext {

	public TestConfiguration config = new TestConfiguration();
	public TestInputProject project = new TestInputProject();
	public TestNewIssue newIssue = new TestNewIssue();
	public TestFileSystem fileSystem = new TestFileSystem();

	@Override
	public Settings settings() {
		return null;
	}

	@Override
	public Configuration config() {
		return config;
	}

	@Override
	public FileSystem fileSystem() {
		return fileSystem;
	}

	@Override
	public ActiveRules activeRules() {
		return null;
	}

	@Override
	public InputModule module() {
		return null;
	}

	@Override
	public InputProject project() {
		return project;
	}

	@Override
	public Version getSonarQubeVersion() {
		return null;
	}

	@Override
	public SonarRuntime runtime() {
		return null;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public <G extends Serializable> NewMeasure<G> newMeasure() {
		TestNewMeasure<G> newMeasure = new TestNewMeasure<>();
		return newMeasure;
	}

	@Override
	public NewIssue newIssue() {
		return newIssue;
	}

	@Override
	public NewExternalIssue newExternalIssue() {
		return null;
	}

	@Override
	public NewAdHocRule newAdHocRule() {
		return null;
	}

	@Override
	public NewHighlighting newHighlighting() {
		return null;
	}

	@Override
	public NewSymbolTable newSymbolTable() {
		return null;
	}

	@Override
	public NewCoverage newCoverage() {
		return null;
	}

	@Override
	public NewCpdTokens newCpdTokens() {
		return null;
	}

	@Override
	public NewAnalysisError newAnalysisError() {
		return null;
	}

	@Override
	public NewSignificantCode newSignificantCode() {
		return null;
	}

	@Override
	public void addContextProperty(String key, String value) {
		// do nothing
	}

	@Override
	public void markForPublishing(InputFile inputFile) {
		// do nothing
	}

}
