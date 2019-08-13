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
package com.overops.plugins.sonar.measures;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.Total_Errors;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.UncaughtExceptionCount;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.SwallowedExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.LogErrorCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.CustomExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.HTTPErrors;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.CaughtExceptionCount;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class MeasureDefinition implements MeasureComputer {
  private static final Logger LOGGER = Loggers.get(MeasureDefinition.class);

  @Override
  public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
    return def.newDefinitionBuilder()
        .setOutputMetrics(Total_Errors.key(), CaughtExceptionCount.key(), UncaughtExceptionCount.key(),
            SwallowedExceptionCount.key(), LogErrorCount.key(), CustomExceptionCount.key(), HTTPErrors.key())
        .build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
    if (context.getComponent().getType() != Component.Type.FILE) {
      context.addMeasure(SwallowedExceptionCount.getKey(), 1);
      context.addMeasure(CaughtExceptionCount.key(), 2);
      context.addMeasure(UncaughtExceptionCount.key(), 3);
      context.addMeasure(CustomExceptionCount.key(), 4);
      context.addMeasure(HTTPErrors.key(), 5);
      context.addMeasure(Total_Errors.key(), 6);
    }
  }
}
