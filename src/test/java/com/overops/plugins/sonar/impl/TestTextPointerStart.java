package com.overops.plugins.sonar.impl;

import org.sonar.api.batch.fs.TextPointer;

public class TestTextPointerStart implements TextPointer {

	@Override
	public int compareTo(TextPointer o) {
		return 0;
	}

	@Override
	public int line() {
		return 1;
	}

	@Override
	public int lineOffset() {
		return 0;
	}

}
