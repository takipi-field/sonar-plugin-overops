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
package org.sonarsource.plugins.OverOps.settings;

import java.util.List;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import static java.util.Arrays.asList;

import java.util.Arrays;

public class OverOpsProperties {

	public static final String OO_URL = "sonar.overops.url";
	public static final String APIKEY = "sonar.overops.apikey";
	public static final String APPHOST = "sonar.overops.host";
	public static final String OO_ENVID = "sonar.overops.envid";
	public static final String APP_NAME = "sonar.overops.applicaiton_name";
	public static final String DEP_NAME = "sonar.overops.deployment_name";
	public static final String TOTAL_ERROR_VOLUME_GATE = "Number";
	public static final String EVENTID = "sonar.overops.eventid";

	public static final String CATEGORY = "OverOps";
	public static final String SUBCATEGORY = "configuration";
	public static final String SUBCATEGORY_QUALITYGATE = "quality_gate";

	private OverOpsProperties() {
		// only statics
	}

	public static List<PropertyDefinition> getProperties() {
		return Arrays.asList(
				PropertyDefinition.builder(OO_URL).name("OverOps URL").description("URL to access GitLab.")
						.category(CATEGORY).subCategory(SUBCATEGORY).index(1).build(),
				PropertyDefinition.builder(APIKEY).name("OverOps API Key")
						.description("OverOps Api Key found in Account").category(CATEGORY).subCategory(SUBCATEGORY)
						.type(PropertyType.PASSWORD).index(2).build(),
				PropertyDefinition.builder(OO_ENVID).name("OverOps Environment ID").category(CATEGORY)
						.subCategory(SUBCATEGORY).type(PropertyType.PASSWORD).index(3).build(),
				PropertyDefinition.builder(APP_NAME).name("Application Name").description("The Agent side App name")
						.category(CATEGORY).subCategory(SUBCATEGORY).index(4).onlyOnQualifiers(Qualifiers.PROJECT)
						.build(),
				PropertyDefinition.builder(DEP_NAME).name("Deployment Name").category(CATEGORY)
						.subCategory(SUBCATEGORY_QUALITYGATE).index(5).type(PropertyType.INTEGER).build(),
				PropertyDefinition.builder(EVENTID).name("Event Id").category(CATEGORY).subCategory(SUBCATEGORY)
						.index(6).onlyOnQualifiers(Qualifiers.PROJECT).build(),
				PropertyDefinition.builder(APPHOST).name("Application Host").category(CATEGORY).subCategory(SUBCATEGORY)
						.index(7).type(PropertyType.STRING).build());
				
	}

}
