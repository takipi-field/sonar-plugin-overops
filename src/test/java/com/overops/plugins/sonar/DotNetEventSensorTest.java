package com.overops.plugins.sonar;

public class DotNetEventSensorTest extends AbstractEventSensorTest {

	@Override
	public EventSensor defineSensor() {
		return new DotNetEventSensor();
	}
}