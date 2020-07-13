package com.overops.plugins.sonar.config;

import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import static java.util.Arrays.asList;

/**
 * Sonar Property Definitions
 */
public class Properties {

	private Properties() {
		// Utility classes should not have public constructors
	}

	// Sonar UI
	public static final String CATEGORY = "OverOps";
	public static final String SUBCATEGORY_GENERAL = "General Settings";
	public static final String SUBCATEGORY_QUALITY_GATE = "Quality Gate Settings";

	// Properties Keys
	public static final String API_URL = "overops.api.url";
	public static final String APP_URL = "overops.app.url";
	public static final String API_KEY = "overops.api.key";
	public static final String ENVIRONMENT_ID = "overops.environment.id";
	public static final String APPLICATION_NAME = "overops.application.name";
	public static final String DEPLOYMENT_NAME = "overops.deployment.name";
	public static final String CRITICAL_EXCEPTION_TYPES = "overops.critical.exception.types";
	public static final String IGNORE_EVENT_TYPES = "overops.ignore.event.types";

	// Property Defaults
	public static final String DEFAULT_API_URL = "https://api.overops.com";
	public static final String DEFAULT_APP_URL = "https://app.overops.com";
	public static final String DEFAULT_APPLICATION_NAME = "All";
	public static final String DEFAULT_CRITICAL_EXCEPTION_TYPES = "NullPointerException,IndexOutOfBoundsException,InvalidCastException,AssertionError";
	public static final String DEFAULT_IGNORE_EVENT_TYPES = "Timer,Logged Warning,Logged Error";

	// Constants
	public static final String VIEW_NAME = "All Events";

	static final PropertyDefinition PROP_API_URL = PropertyDefinition.builder(API_URL)
			.name("API URL")
			.description("The complete URL including port and protocol of the OverOps API (e.g. https://api.overops.com or http://host.domain.com:8080)")
			.defaultValue(DEFAULT_API_URL)
			.category(CATEGORY)
			.subCategory(SUBCATEGORY_GENERAL)
			.type(PropertyType.STRING)
			.onQualifiers(Qualifiers.PROJECT)
			.index(1)
			.build();

	static final PropertyDefinition PROP_APP_URL = PropertyDefinition.builder(APP_URL)
			.name("App URL")
			.description("The complete URL including port and protocol of the OverOps UI (e.g. https://app.overops.com or http://host.domain.com:8080)")
			.defaultValue(DEFAULT_APP_URL)
			.category(CATEGORY)
			.subCategory(SUBCATEGORY_GENERAL)
			.type(PropertyType.STRING)
			.onQualifiers(Qualifiers.PROJECT)
			.index(2)
			.build();

	static final PropertyDefinition PROP_API_KEY = PropertyDefinition.builder(API_KEY)
			.name("API Token")
			.description("The OverOps REST API token to use for authentication. This can be obtained from the OverOps dashboard under Settings -> Account.")
			.category(CATEGORY)
			.subCategory(SUBCATEGORY_GENERAL)
			.type(PropertyType.PASSWORD)
			.onQualifiers(Qualifiers.PROJECT)
			.index(3)
			.build();

	static final PropertyDefinition PROP_ENVIRONMENT_ID = PropertyDefinition.builder(ENVIRONMENT_ID)
			.name("Environment ID")
			.description("The OverOps environment identifier (e.g. S12345).")
			.category(CATEGORY)
			.subCategory(SUBCATEGORY_GENERAL)
			.type(PropertyType.STRING)
			.onQualifiers(Qualifiers.PROJECT)
			.index(4)
			.build();

	static final PropertyDefinition PROP_APPLICATION_NAME = PropertyDefinition.builder(APPLICATION_NAME)
			.name("Application Name")
			.description("(Optional) Application Name as specified in OverOps.")
			.category(CATEGORY)
			.subCategory(SUBCATEGORY_GENERAL)
			.type(PropertyType.STRING)
			.onQualifiers(Qualifiers.PROJECT)
			.index(5)
			.build();

	static final PropertyDefinition PROP_DEPLOYMENT_NAME = PropertyDefinition.builder(DEPLOYMENT_NAME)
			.name("Deployment Name")
			.description("Deployment Name as specified in OverOps. Defaults to sonar.buildString.")
			.category(CATEGORY)
			.subCategory(SUBCATEGORY_GENERAL)
			.type(PropertyType.STRING)
			.onQualifiers(Qualifiers.PROJECT)
			.index(6)
			.build();

	static final PropertyDefinition PROP_CRITICAL_EXCEPTION_TYPES = PropertyDefinition.builder(CRITICAL_EXCEPTION_TYPES)
			.name("Critical Exception Types")
			.description("A comma delimited list of critical exception types (e.g. NullPointerException,IndexOutOfBoundsException)")
			.defaultValue(DEFAULT_CRITICAL_EXCEPTION_TYPES)
			.category(CATEGORY)
			.subCategory(SUBCATEGORY_QUALITY_GATE)
			.type(PropertyType.STRING)
			.onQualifiers(Qualifiers.PROJECT)
			.index(7)
			.build();

	static final PropertyDefinition PROP_IGNORE_EVENT_TYPES = PropertyDefinition.builder(IGNORE_EVENT_TYPES)
			.name("Ignore Event Types")
			.description("A comma delimited list of specific types of events to ignore (e.g. Timer,Logged Warning,Logged Error)")
			.defaultValue(DEFAULT_IGNORE_EVENT_TYPES)
			.category(CATEGORY)
			.subCategory(SUBCATEGORY_QUALITY_GATE)
			.type(PropertyType.STRING)
			.onQualifiers(Qualifiers.PROJECT)
			.index(7)
			.build();

	public static List<PropertyDefinition> getProperties() {
		return asList(
				PROP_API_URL,
				PROP_APP_URL,
				PROP_API_KEY,
				PROP_ENVIRONMENT_ID,
				PROP_APPLICATION_NAME,
				PROP_DEPLOYMENT_NAME,
				PROP_CRITICAL_EXCEPTION_TYPES,
				PROP_IGNORE_EVENT_TYPES
		);
	}
}
