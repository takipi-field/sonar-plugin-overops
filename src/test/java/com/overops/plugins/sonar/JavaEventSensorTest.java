package com.overops.plugins.sonar;

public class JavaEventSensorTest extends AbstractEventSensorTest {


	@Override
	public EventSensor defineSensor() {
		return new JavaEventSensor();
	}
}