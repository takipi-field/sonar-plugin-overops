package com.overops.plugins.sonar.impl;

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class TestNewIssueLocation implements NewIssueLocation {

	@Override
	public NewIssueLocation on(InputComponent component) {
		return this;
	}

	@Override
	public NewIssueLocation at(TextRange location) {
		return this;
	}

	@Override
	public NewIssueLocation message(String message) {
		return this;
	}

}
