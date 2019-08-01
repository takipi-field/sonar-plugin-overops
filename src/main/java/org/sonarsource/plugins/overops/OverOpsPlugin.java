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
package org.sonarsource.plugins.overops;

import static java.util.Arrays.asList;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonarsource.plugins.overops.measures.ComputeSizeAverage;
import org.sonarsource.plugins.overops.measures.ComputeSizeRating;
import org.sonarsource.plugins.overops.measures.OOSensor;
import org.sonarsource.plugins.overops.measures.OverOpsMetrics;
import org.sonarsource.plugins.overops.measures.SetSizeOnFilesSensor;
import org.sonarsource.plugins.overops.settings.OverOpsProperties;
import org.sonarsource.plugins.overops.web.MyPluginPageDefinition;

/**
 * This class is the entry point for all extensions. It is referenced in
 * pom.xml.
 */
public class OverOpsPlugin implements Plugin {

	@Override
	public void define(Context context) {
		// tutorial on measures
		context.addExtensions(OverOpsMetrics.class, SetSizeOnFilesSensor.class, ComputeSizeAverage.class,
				ComputeSizeRating.class);

		// tutorial on settings
		context.addExtensions(OverOpsProperties.getProperties());

		// tutorial on web extensions
		context.addExtension(MyPluginPageDefinition.class);
		
		context.addExtension(OOSensor.class);
	}

}
