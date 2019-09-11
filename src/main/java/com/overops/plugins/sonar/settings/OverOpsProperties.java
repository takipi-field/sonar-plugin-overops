package com.overops.plugins.sonar.settings;

import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.Builder;
import org.sonar.api.resources.Qualifiers;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;

public class OverOpsProperties {

	public static final String SONAR_OVEROPS_VIEWID = "sonar.overops.viewid";
	public static String ooURL = "sonar.overops.url";
	public static String apiKEY = "sonar.overops.apikey";
	public static String ooENVID = "sonar.overops.environmentId";
	public static String appNAME = "sonar.overops.applicaiton_name";
	public static String depNAME = "sonar.overops.deployment_name";
	public static String eventID = "sonar.overops.eventid";
	public static String versionNum = "sonar.overops.versionNum";
	public static String DAYS = "1";

	public static String CATEGORY = "OverOps";
	public static String SUBCATEGORY = "Settings";

	/* The Settings Panel inside of the administration column */
	public static List<PropertyDefinition> getProperties() {
		return Arrays.asList(
				PropertyDefinition.builder(apiKEY).name("OverOps API Key")
						.description("OverOps Api Key found in Account").category(CATEGORY).subCategory(SUBCATEGORY)
						.type(PropertyType.PASSWORD).index(1).build(),

				PropertyDefinition.builder(ooURL).name("OverOps URL").description("OverOps URL for API endpoint")
						.defaultValue("https://api.overops.com").category(CATEGORY).subCategory(SUBCATEGORY)
						.type(PropertyType.STRING).index(2).build(),

				PropertyDefinition.builder(ooENVID).name("OverOps Environment ID").category(CATEGORY)
						.subCategory(SUBCATEGORY).type(PropertyType.STRING).index(3).build(),

				PropertyDefinition.builder(DAYS).name("Days to Pull Back").defaultValue("1").category(CATEGORY)
						.subCategory(SUBCATEGORY).type(PropertyType.INTEGER).index(4)
						.description("Enter the amount of days to pull back default is 1").build(),

				PropertyDefinition.builder(appNAME).name("Application Name").category(CATEGORY)
						.subCategory(SUBCATEGORY).onQualifiers(Qualifiers.APP).type(PropertyType.STRING).index(5)
						.build(),
				PropertyDefinition.builder(depNAME).name("Deployment Name").category(CATEGORY).subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.APP).type(PropertyType.STRING).index(6).build(),

				PropertyDefinition.builder(SONAR_OVEROPS_VIEWID).name("View")
						.description("View name").category(CATEGORY).subCategory(SUBCATEGORY)
						.onQualifiers(Qualifiers.APP).type(PropertyType.STRING).index(7).build());

	}

}
