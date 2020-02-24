package com.overops.plugins.sonar.impl;

import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

public class TestTextRange implements TextRange {

	TextPointer start = new TestTextPointerStart();
	TextPointer end = new TestTextPointerEnd();

	@Override
	public TextPointer start() {
		return start;
	}

	@Override
	public TextPointer end() {
		return end;
	}

	@Override
	public boolean overlap(TextRange another) {
		return false;
	}

}
