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
package org.sonarsource.plugins.overops.measures;

import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.event_list_size;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.Total_Unique_Errors;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.UncaughtExceptionCount;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.SwallowedExceptionCount;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.LogErrorCount;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.CustomExceptionCount;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.HTTPErrors;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.MeasureComputer;

public class ComputeSizeAverage implements MeasureComputer {

  @Override
  public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
    return def.newDefinitionBuilder()
      .setOutputMetrics(event_list_size.key(), Total_Unique_Errors.key(), UncaughtExceptionCount.key(), SwallowedExceptionCount.key(), LogErrorCount.key(), CustomExceptionCount.key(), HTTPErrors.key()).build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
  }
}
