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
        .setOutputMetrics(CaughtExceptionCount.key(), UncaughtExceptionCount.key(),
            SwallowedExceptionCount.key(), LogErrorCount.key(), CustomExceptionCount.key(), HTTPErrors.key())
        .build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
    if (context.getComponent().getType() != Component.Type.FILE) {
      int sum = 0; 
      for(Measure measure : context.getChildrenMeasures(SwallowedExceptionCount.key())){
        sum += measure.getIntValue();
      }
      int sum_Uncaught = 0;
      for(Measure measure : context.getChildrenMeasures(UncaughtExceptionCount.key())){
        sum_Uncaught += measure.getIntValue();
      }
      int sum_Custom = 0;
      for(Measure measure : context.getChildrenMeasures(CustomExceptionCount.key())){
        sum_Custom += measure.getIntValue();
      }
      int sum_Caught = 0;
      for(Measure measure : context.getChildrenMeasures(CaughtExceptionCount.key())){
        sum_Caught += measure.getIntValue();
      }
      int sum_HTTP = 0;
      for(Measure measure : context.getChildrenMeasures(HTTPErrors.key())){
        sum_HTTP += measure.getIntValue();
      }
      int sum_Log = 0;
      for(Measure measure : context.getChildrenMeasures(LogErrorCount.key())){
        sum_Log += measure.getIntValue();
      }

      context.addMeasure(SwallowedExceptionCount.key(), sum);
      context.addMeasure(UncaughtExceptionCount.getKey(), sum_Uncaught);
      context.addMeasure(CaughtExceptionCount.key(), sum_Caught);
      context.addMeasure(CustomExceptionCount.key(), sum_Custom);
      context.addMeasure(HTTPErrors.key(), sum_HTTP);
      context.addMeasure(LogErrorCount.key(), sum_Log);
    }
  }
}
