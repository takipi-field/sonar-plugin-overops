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

import java.util.List;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static java.util.Arrays.asList;

public class OverOpsMetrics implements Metrics {

  public static final Metric<Integer> FILENAME_SIZE = new Metric.Builder("overops_key", "OverOps Measures", Metric.ValueType.INT)
    .setDescription("OverOps configuration form ")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(false)
    .setDomain(CoreMetrics.DOMAIN_RELIABILITY)
    .create();

  public static final Metric<Integer> FILENAME_SIZE_RATING = new Metric.Builder("overops_reliability_report", "OverOps Reliability Form", Metric.ValueType.RATING)
    .setDescription("Report after configuring")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(CoreMetrics.DOMAIN_RELIABILITY)
    .create();

    public static final Metric<Integer> NEW_ERROR_COUNT = new Metric.Builder("overops_new_error_count", "New Errors", Metric.ValueType.INT)
    .setDescription("New errors")
    .setQualitative(false)
    .setDomain(CoreMetrics.DOMAIN_RELIABILITY)
    .create();

  @Override
  public List<Metric> getMetrics() {
    return asList(FILENAME_SIZE, FILENAME_SIZE_RATING, NEW_ERROR_COUNT);
  }
}
