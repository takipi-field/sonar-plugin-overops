package com.overops.plugins.sonar.impl;

import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public class TestNewIssue implements NewIssue {

	TestNewIssueLocation newIssueLocation = new TestNewIssueLocation();

	@Override
	public NewIssue forRule(RuleKey ruleKey) {
		return this;
	}

	@Override
	public NewIssue gap(Double gap) {
		return this;
	}

	@Override
	public NewIssue overrideSeverity(Severity severity) {
		return this;
	}

	@Override
	public NewIssue at(NewIssueLocation primaryLocation) {
		return this;
	}

	@Override
	public NewIssue addLocation(NewIssueLocation secondaryLocation) {
		return this;
	}

	@Override
	public NewIssue addFlow(Iterable<NewIssueLocation> flowLocations) {
		return this;
	}

	@Override
	public NewIssueLocation newLocation() {
		return newIssueLocation;
	}

	@Override
	public void save() {
		// do nothing
	}

}
