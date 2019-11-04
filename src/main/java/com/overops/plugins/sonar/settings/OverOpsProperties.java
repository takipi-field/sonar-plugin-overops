package com.overops.plugins.sonar.settings;

import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;

public class OverOpsProperties {
    public static final String CATEGORY = "OverOps";
    public static final String SUBCATEGORY = "Settings";

	public static final String SONAR_OVEROPS_VIEW_ID = "sonar.overops.viewId";
	public static final String SONAR_OVEROPS_API_HOST = "sonar.overops.apiHost";
	public static final String SONAR_OVEROPS_API_KEY = "sonar.overops.apikey";
	public static final String SONAR_OVEROPS_ENVIRONMENT_ID = "sonar.overops.environmentId";
	public static final String SONAR_OVEROPS_APP_NAME = "sonar.overops.applicationName";
	public static final String SONAR_OVEROPS_DEP_NAME = "sonar.overops.deploymentName";
	public static final String SONAR_OVEROPS_SPAN_DAYS = "sonar.overops.spanDays";
	public static final String SONAR_OVEROPS_USER_NAME = "sonar.overops.userName";
	public static final String SONAR_OVEROPS_USER_PASSWORD = "sonar.overops.userPassword";

	public static List<PropertyDefinition> getProperties() {
		return Arrays.asList(
				PropertyDefinition.builder(SONAR_OVEROPS_API_KEY)
                        .name("OverOps API Key")
						.description("OverOps Api Key found in Account")
                        .category(CATEGORY)
                        .subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.PROJECT)
						.type(PropertyType.PASSWORD)
                        .index(1)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_API_HOST)
                        .name("OverOps URL")
                        .description("OverOps URL for API endpoint")
						.defaultValue("https://api.overops.com")
                        .category(CATEGORY)
                        .subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.PROJECT)
						.type(PropertyType.STRING)
                        .index(2)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_ENVIRONMENT_ID)
                        .name("OverOps Environment ID")
                        .category(CATEGORY)
						.subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.PROJECT)
                        .type(PropertyType.STRING)
                        .index(3)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_SPAN_DAYS)
                        .name("Days to Pull Back")
                        .defaultValue("1")
                        .category(CATEGORY)
						.subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.PROJECT)
                        .type(PropertyType.INTEGER)
                        .index(4)
						.description("Enter the amount of days to pull back default is 1")
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_APP_NAME)
                        .name("Application Name")
                        .category(CATEGORY)
						.subCategory(SUBCATEGORY)
                        .onQualifiers(Qualifiers.PROJECT)
                        .type(PropertyType.STRING)
                        .index(5)
						.build(),

				PropertyDefinition.builder(SONAR_OVEROPS_DEP_NAME)
                        .name("Deployment Name")
                        .category(CATEGORY)
                        .subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.PROJECT)
                        .type(PropertyType.STRING)
                        .index(6)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_VIEW_ID)
                        .name("View")
						.description("View name")
                        .category(CATEGORY)
                        .subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.PROJECT)
                        .type(PropertyType.STRING)
                        .index(7)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_USER_NAME)
						.name("User name")
						.description("User name, which has rights to add/delete/comment issues")
						.category(CATEGORY)
						.subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.PROJECT)
						.type(PropertyType.STRING)
						.defaultValue("admin")
						.index(8)
						.build(),
				PropertyDefinition.builder(SONAR_OVEROPS_USER_PASSWORD)
						.name("User password")
						.description("User password, which has rights to add/delete/comment issues")
						.category(CATEGORY)
						.subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.PROJECT)
						.type(PropertyType.STRING)
						.defaultValue("admin")
						.index(9)
						.build());

	}
}
