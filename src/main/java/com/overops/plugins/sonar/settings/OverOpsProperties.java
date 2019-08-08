/*
 * Example Plugin for SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.overops.plugins.sonar.settings;

import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;

public class OverOpsProperties {

	public static String OO_URL = "sonar.overops.url";
	public static String APIKEY = "sonar.overops.apikey";
	public static String OO_ENVID = "sonar.overops.environmentId";
	public static String APP_NAME = "sonar.overops.applicaiton_name";
	public static String DEP_NAME = "sonar.overops.deployment_name";
	public static String EVENTID = "sonar.overops.eventid";
	public static String versionNum = "sonar.overops.versionNum";
	public static String DAYS = "1";

	public static String CATEGORY = "OverOps";
	public static String SUBCATEGORY = "Settings";
	public static String SUBCATEGORY_QUALITYGATE = "quality_gate";

	public static List<PropertyDefinition> getProperties() {
		return Arrays.asList(
				PropertyDefinition.builder(APIKEY).name("OverOps API Key")
						.description("OverOps Api Key found in Account").category(CATEGORY).subCategory(SUBCATEGORY)
						.type(PropertyType.PASSWORD).index(1).build(),
				PropertyDefinition.builder(OO_URL).name("OverOps URL").description("OverOps URL for API endpoint")
						.defaultValue("https://api.overops.com").category(CATEGORY).subCategory(SUBCATEGORY)
						.type(PropertyType.STRING).index(2).build(),
				PropertyDefinition.builder(OO_ENVID).name("OverOps Environment ID").category(CATEGORY)
						.subCategory(SUBCATEGORY).type(PropertyType.STRING).index(3).build(),
				PropertyDefinition.builder(DAYS).name("Days to look back").defaultValue("1").category(CATEGORY)
						.subCategory(SUBCATEGORY).type(PropertyType.INTEGER).index(4).description("Enter the amount of days to pull back default is 1").build(),
				PropertyDefinition.builder(APP_NAME).name("Application Name").category(CATEGORY).subCategory(SUBCATEGORY).onQualifiers(Qualifiers.PROJECT)
						.type(PropertyType.STRING).index(5).build(),
				PropertyDefinition.builder(DEP_NAME).onQualifiers(Qualifiers.PROJECT).name("Deployment Name").category(CATEGORY).subCategory(SUBCATEGORY)
						.type(PropertyType.STRING).index(6).build());

	}

}
