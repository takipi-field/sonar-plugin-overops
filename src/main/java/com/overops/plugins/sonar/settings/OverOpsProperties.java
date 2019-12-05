package com.overops.plugins.sonar.settings;

import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;

import static com.overops.plugins.sonar.OverOpsPlugin.DEFAULT_VIEWID;

public class OverOpsProperties {
    public static final String CATEGORY = "OverOps";
    public static final String SETTINGS_SUBCATEGORY = "Account Settings";
	public static final String QUALITY_GATE_OPTIONS_SUBCATEGORY = "Quality Gate Options";

	public static final String SONAR_OVEROPS_VIEW_ID = "sonar.overops.viewId";
	public static final String SONAR_OVEROPS_API_HOST = "sonar.overops.apiHost";
    public static final String SONAR_OVEROPS_APP_HOST = "sonar.overops.appHost";
	public static final String SONAR_OVEROPS_API_KEY = "sonar.overops.apikey";
	public static final String SONAR_OVEROPS_ENVIRONMENT_ID = "sonar.overops.environmentId";
	public static final String SONAR_OVEROPS_APP_NAME = "sonar.overops.applicationName";
	public static final String SONAR_OVEROPS_DEP_NAME = "sonar.overops.deploymentName";
	public static final String SONAR_OVEROPS_SPAN_DAYS = "sonar.overops.spanDays";
	public static final String SONAR_OVEROPS_USER_NAME = "sonar.overops.userName";
	public static final String SONAR_OVEROPS_USER_PASSWORD = "sonar.overops.userPassword";
	public static final String SONAR_OVEROPS_NEW_ERROR_GATE = "sonar.overops.new.error.gate";
	public static final String SONAR_OVEROPS_RESURFACED_ERROR_GATE = "sonar.overops.resurfaced.error.gate";
	public static final String SONAR_OVEROPS_CRITICAL_ERROR_GATE = "sonar.overops.critical.error.gate";
	public static final String SONAR_OVEROPS_INCREASING_ERROR_GATE = "sonar.overops.increasing.error.gate";
	public static final String SONAR_OVEROPS_INCREASING_ACTIVE_TIME_WINDOW = "sonar.overops.increasing.active.time.window";
	public static final String SONAR_OVEROPS_INCREASING_BASELINE_TIME_WINDOW = "sonar.overops.increasing.base.time.window";
	public static final String SONAR_OVEROPS_INCREASING_ERROR_VOLUME_THRESHOLD = "sonar.overops.increasing.error.volume.threshold";
	public static final String SONAR_OVEROPS_INCREASING_ERROR_RATE_THRESHOLD = "sonar.overops.increasing.error.rate.threshold";
	public static final String SONAR_OVEROPS_INCREASING_REGRESSION_DELTA = "sonar.overops.increasing.regression.delta";
	public static final String SONAR_OVEROPS_INCREASING_CRITICAL_REGRESSION_THRESHOLD = "sonar.overops.increasing.critical.regression.threshold";
	public static final String SONAR_OVEROPS_INCREASING_APPLY_SEASONALITY = "sonar.overops.increasing.apply.seasonality";
	public static final String BASELINE_TIME_WINDOW_DEFAULT = "7d";
	public static final String ERROR_VOLUME_THRESHOLD_DEFAULT = "20";
	public static final String ERROR_RATE_THRESHOLD_DEFAULT = "0.1";
	public static final String REGRESSION_DELTA_DEFAULT = "0.5";
	public static final String CRITICAL_REGRESSION_THRESHOLD_DEFAULT = "1";
	public static final String APPLY_SEASONALITY_DEFAULT = "true";
	public static final String ACTIVE_TIME_WINDOW_DEFAULT = "12h";
	public static final String INCREASING_ERROR_GATE_DEFAULT = "true";
	public static final String CRITICAL_EXCEPTION_TYPES_DEFAULT = "";
	public static final String RESURFACED_ERROR_GATE_DEFAULT = "true";
	public static final String NEW_ERRORS_GATE_DEFAULT = "true";


	public static List<PropertyDefinition> getProperties() {
		return Arrays.asList(
				PropertyDefinition.builder(SONAR_OVEROPS_API_KEY)
                        .name("OverOps API Key")
						.description("OverOps Api Key found in Account")
                        .category(CATEGORY)
                        .subCategory(SETTINGS_SUBCATEGORY)
						.type(PropertyType.PASSWORD)
                        .index(1)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_API_HOST)
                        .name("OverOps URL")
                        .description("OverOps URL for API endpoint")
						.defaultValue("https://api.overops.com")
                        .category(CATEGORY)
                        .subCategory(SETTINGS_SUBCATEGORY)
						.type(PropertyType.STRING)
                        .index(2)
                        .build(),

                PropertyDefinition.builder(SONAR_OVEROPS_APP_HOST)
                        .name("OverOps URL")
                        .description("OverOps URL for APP endpoint")
                        .defaultValue("https://app.overops.com")
                        .category(CATEGORY)
                        .subCategory(SETTINGS_SUBCATEGORY)
                        .type(PropertyType.STRING)
                        .index(2)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_ENVIRONMENT_ID)
                        .name("OverOps Environment ID")
                        .category(CATEGORY)
						.subCategory(SETTINGS_SUBCATEGORY)
                        .type(PropertyType.STRING)
                        .index(3)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_SPAN_DAYS)
                        .name("Days to Pull Back")
                        .defaultValue("1")
                        .category(CATEGORY)
						.subCategory(SETTINGS_SUBCATEGORY)
                        .type(PropertyType.INTEGER)
                        .index(4)
						.description("Enter the amount of days to pull back default is 1")
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_APP_NAME)
                        .name("Application Name")
                        .category(CATEGORY)
						.subCategory(SETTINGS_SUBCATEGORY)
                        .type(PropertyType.STRING)
                        .index(5)
						.build(),

				PropertyDefinition.builder(SONAR_OVEROPS_DEP_NAME)
                        .name("Deployment Name")
                        .category(CATEGORY)
                        .subCategory(SETTINGS_SUBCATEGORY)
                        .type(PropertyType.STRING)
                        .index(6)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_VIEW_ID)
                        .name("View")
						.description("View name")
                        .category(CATEGORY)
                        .subCategory(SETTINGS_SUBCATEGORY)
                        .type(PropertyType.STRING)
						.defaultValue(DEFAULT_VIEWID)
                        .index(7)
                        .build(),

				PropertyDefinition.builder(SONAR_OVEROPS_USER_NAME)
						.name("User name")
						.description("User name, which has rights to add/delete/comment issues")
						.category(CATEGORY)
						.subCategory(SETTINGS_SUBCATEGORY)
						.type(PropertyType.STRING)
						.defaultValue("admin")
						.index(8)
						.build(),
				PropertyDefinition.builder(SONAR_OVEROPS_USER_PASSWORD)
						.name("User password")
						.description("User password, which has rights to add/delete/comment issues")
						.category(CATEGORY)
						.subCategory(SETTINGS_SUBCATEGORY)
						.type(PropertyType.STRING)
						.defaultValue("admin")
						.index(9)
						.build()
				//TODO uncomment when we find way how to apply those values to Reliability report
//				,
//				PropertyDefinition.builder(SONAR_OVEROPS_NEW_ERROR_GATE)
//						.name("New Error Gate")
//						.description("Detect New Errors")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.BOOLEAN)
//						.defaultValue(NEW_ERRORS_GATE_DEFAULT)
//						.index(10)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_RESURFACED_ERROR_GATE)
//						.name("Resurfaced Error Gate")
//						.description("Detect Resurfaced Errors")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.BOOLEAN)
//						.defaultValue(RESURFACED_ERROR_GATE_DEFAULT)
//						.index(10)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_CRITICAL_ERROR_GATE)
//						.name("Critical Exception Type(s) Gate")
//						.description("Detect Critical Exception Types. If empty gate isn't used")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.TEXT)
//						.defaultValue(CRITICAL_EXCEPTION_TYPES_DEFAULT)
//						.index(11)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_INCREASING_ERROR_GATE)
//						.name("Increasing Errors Gate")
//						.description("Detect Increasing Errors")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.BOOLEAN)
//						.defaultValue(INCREASING_ERROR_GATE_DEFAULT)
//						.index(12)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_INCREASING_ACTIVE_TIME_WINDOW)
//						.name("Active Time Window")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.STRING)
//						.defaultValue(ACTIVE_TIME_WINDOW_DEFAULT)
//						.index(13)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_INCREASING_BASELINE_TIME_WINDOW)
//						.name("Baseline Time Window")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.STRING)
//						.defaultValue(BASELINE_TIME_WINDOW_DEFAULT)
//						.index(14)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_INCREASING_ERROR_VOLUME_THRESHOLD)
//						.name("Error Volume Threshold")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.INTEGER)
//						.defaultValue(ERROR_VOLUME_THRESHOLD_DEFAULT)
//						.index(15)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_INCREASING_ERROR_RATE_THRESHOLD)
//						.name("Error Rate Threshold (0-1)")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.FLOAT)
//						.defaultValue(ERROR_RATE_THRESHOLD_DEFAULT)
//						.index(16)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_INCREASING_REGRESSION_DELTA)
//						.name("Regression Delta (0-1)")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.FLOAT)
//						.defaultValue(REGRESSION_DELTA_DEFAULT)
//						.index(17)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_INCREASING_CRITICAL_REGRESSION_THRESHOLD)
//						.name("Critical Regression Threshold (0-1)")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.INTEGER)
//						.defaultValue(CRITICAL_REGRESSION_THRESHOLD_DEFAULT)
//						.index(18)
//						.build(),
//				PropertyDefinition.builder(SONAR_OVEROPS_INCREASING_APPLY_SEASONALITY)
//						.name("Apply Seasonality")
//						.category(CATEGORY)
//						.subCategory(QUALITY_GATE_OPTIONS_SUBCATEGORY)
//						.type(PropertyType.BOOLEAN)
//						.defaultValue(APPLY_SEASONALITY_DEFAULT)
//						.index(19)
//						.build()
		);


	}
}
