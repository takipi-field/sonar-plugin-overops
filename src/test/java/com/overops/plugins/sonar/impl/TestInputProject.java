package com.overops.plugins.sonar.impl;

import org.sonar.api.scanner.fs.InputProject;

public class TestInputProject implements InputProject {

	@Override
	public String key() {
		return "test";
	}

	@Override
	public boolean isFile() {
		return false;
	}

}
