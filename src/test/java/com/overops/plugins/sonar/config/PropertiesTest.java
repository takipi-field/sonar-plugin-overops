package com.overops.plugins.sonar.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PropertiesTest {

	@Test
	public void constructor() {
		assertThrows(IllegalStateException.class, () -> new Properties());
	}
}