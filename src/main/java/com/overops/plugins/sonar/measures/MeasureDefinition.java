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

import static com.overops.plugins.sonar.measures.OverOpsMetrics.UncaughtExceptionCount;

import static com.overops.plugins.sonar.measures.OverOpsMetrics.SwallowedExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.LogErrorCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.CustomExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.HTTPErrors;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.CaughtExceptionCount;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.CriticalExceptionCount;

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
        .setOutputMetrics(CaughtExceptionCount.key(), UncaughtExceptionCount.key(), SwallowedExceptionCount.key(),
            LogErrorCount.key(), CustomExceptionCount.key(), HTTPErrors.key(), CriticalExceptionCount.key())
        .build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
    if (context.getComponent().getType() != Component.Type.FILE) {
      int sum = 0;
      for (Measure measure : context.getChildrenMeasures(SwallowedExceptionCount.key())) {
        sum += measure.getIntValue();
      }
      int sum_Uncaught = 0;
      for (Measure measure : context.getChildrenMeasures(UncaughtExceptionCount.key())) {
        sum_Uncaught += measure.getIntValue();
      }
      int sumCustom = 0;
      for (Measure measure : context.getChildrenMeasures(CustomExceptionCount.key())) {
        sumCustom += measure.getIntValue();
      }
      int sumCaught = 0;
      for (Measure measure : context.getChildrenMeasures(CaughtExceptionCount.key())) {
        sumCaught += measure.getIntValue();
      }
      int sumHTTP = 0;
      for (Measure measure : context.getChildrenMeasures(HTTPErrors.key())) {
        sumHTTP += measure.getIntValue();
      }
      int sumLog = 0;
      for (Measure measure : context.getChildrenMeasures(LogErrorCount.key())) {
        sumLog += measure.getIntValue();
      }
      int sumCritException = 0;
      for (Measure measure : context.getChildrenMeasures(CriticalExceptionCount.key())) {
        sumCritException += measure.getIntValue();
      }

      context.addMeasure(CriticalExceptionCount.key(), sumCritException);
      context.addMeasure(SwallowedExceptionCount.key(), sum);
      context.addMeasure(UncaughtExceptionCount.getKey(), sum_Uncaught);
      context.addMeasure(CaughtExceptionCount.key(), sumCaught);
      context.addMeasure(CustomExceptionCount.key(), sumCustom);
      context.addMeasure(HTTPErrors.key(), sumHTTP);
      context.addMeasure(LogErrorCount.key(), sumLog);
    }
  }
}
