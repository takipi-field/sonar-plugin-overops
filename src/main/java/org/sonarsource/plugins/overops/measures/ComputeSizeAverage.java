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

import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.FILENAME_SIZE;
import static org.sonarsource.plugins.overops.measures.OverOpsMetrics.NEW_ERROR_COUNT;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;

public class ComputeSizeAverage implements MeasureComputer {

  @Override
  public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
    return def.newDefinitionBuilder()
      .setOutputMetrics(FILENAME_SIZE.key(), NEW_ERROR_COUNT.key())
      .build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
    // measure is already defined on files by {@link SetSizeOnFilesSensor}
    // in scanner stack

    // context.getComponent().getType() == Component.Type.MODULE
    // Component.Type.PROJECT
    if (context.getComponent().getType() == Component.Type.PROJECT) {
      context.addMeasure(FILENAME_SIZE.key(), 7777);
      context.addMeasure(NEW_ERROR_COUNT.key(), 1000000);
    }

    if (context.getComponent().getType() != Component.Type.FILE &&
        context.getComponent().getType() != Component.Type.PROJECT) {
      int sum = 0;
      int count = 0;
      for (Measure child : context.getChildrenMeasures(FILENAME_SIZE.key())) {
        sum += child.getIntValue();
        count++;
      }
      // int average = count == 0 ? 0 : sum / count;
      // context.addMeasure(FILENAME_SIZE.key(), average);
      context.addMeasure(FILENAME_SIZE.key(), 42);
    }
  }
}
