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
package org.sonarsource.plugins.OverOps;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonarsource.plugins.OverOps.hooks.DisplayIssuesInScanner;
import org.sonarsource.plugins.OverOps.hooks.DisplayQualityGateStatus;
import org.sonarsource.plugins.OverOps.languages.FooLanguage;
import org.sonarsource.plugins.OverOps.languages.FooQualityProfile;
import org.sonarsource.plugins.OverOps.measures.ComputeSizeAverage;
import org.sonarsource.plugins.OverOps.measures.ComputeSizeRating;
import org.sonarsource.plugins.OverOps.measures.OverOpsMetrics;
import org.sonarsource.plugins.OverOps.measures.SetSizeOnFilesSensor;
import org.sonarsource.plugins.OverOps.rules.CreateIssuesOnJavaFilesSensor;
import org.sonarsource.plugins.OverOps.rules.FooLintIssuesLoaderSensor;
import org.sonarsource.plugins.OverOps.rules.FooLintRulesDefinition;
import org.sonarsource.plugins.OverOps.rules.JavaRulesDefinition;
import org.sonarsource.plugins.OverOps.settings.FooLanguageProperties;
import org.sonarsource.plugins.OverOps.settings.HelloWorldProperties;
import org.sonarsource.plugins.OverOps.settings.SayHelloFromScanner;
import org.sonarsource.plugins.OverOps.web.MyPluginPageDefinition;

import static java.util.Arrays.asList;

/**
 * This class is the entry point for all extensions. It is referenced in pom.xml.
 */
public class OverOpsPlugin implements Plugin {

  @Override
  public void define(Context context) {
    // tutorial on hooks
    // http://docs.sonarqube.org/display/DEV/Adding+Hooks
    context.addExtensions(DisplayIssuesInScanner.class, DisplayQualityGateStatus.class);

    // tutorial on languages
    context.addExtensions(FooLanguage.class, FooQualityProfile.class);
    context.addExtension(FooLanguageProperties.getProperties());

    // tutorial on measures
    context
      .addExtensions(OverOpsMetrics.class, SetSizeOnFilesSensor.class, ComputeSizeAverage.class, ComputeSizeRating.class);

    // tutorial on rules
    context.addExtensions(JavaRulesDefinition.class, CreateIssuesOnJavaFilesSensor.class);
    context.addExtensions(FooLintRulesDefinition.class, FooLintIssuesLoaderSensor.class);

    // tutorial on settings
    context
      .addExtensions(HelloWorldProperties.getProperties())
      .addExtension(SayHelloFromScanner.class);

    // tutorial on web extensions
    context.addExtension(MyPluginPageDefinition.class);

    context.addExtensions(asList(
      PropertyDefinition.builder("sonar.foo.file.suffixes")
        .name("Suffixes FooLint")
        .description("Suffixes supported by FooLint")
        .category("FooLint")
        .defaultValue("")
        .build()));
  }
}
