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

import java.util.List;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static java.util.Arrays.asList;
/*
*Class is strictly for defining Metrics to display, all of the metrics will be accessible for any of the graphs once defined and populated
*/
public class OverOpsMetrics implements Metrics {

  public static String OO_DOMAIN = "OverOps Exceptions";

    public static final Metric<Integer> Total_Errors = new Metric.Builder("overops_num_unique_errors", "Total Errors ", Metric.ValueType.INT)
    .setDescription("Total Errors")
    .setQualitative(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(OO_DOMAIN)
    .setBestValue(0.0)
    .create();

    public static final Metric<Integer> CaughtExceptionCount = new Metric.Builder("overops_caught_exception", "Caught Exceptions", Metric.ValueType.INT)
    .setDescription("Caught Exception Count")
    .setQualitative(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(OO_DOMAIN)
    .setBestValue(0.0)
    .create();

    public static final Metric<Integer> Total_Unique_Errors = new Metric.Builder("overops_total_volume_errors", "Total Unique Errors", Metric.ValueType.INT)
    .setDescription("Unique Errors found")
    .setQualitative(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(OO_DOMAIN)
    .setBestValue(0.0)
    .create();

    public static final Metric<Integer> UncaughtExceptionCount = new Metric.Builder("overops_uncaught_exceptions", "Uncaught Exceptions", Metric.ValueType.INT)
    .setDescription("Number of Uncaught Exception")
    .setQualitative(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(OO_DOMAIN)
    .setBestValue(0.0)
    .create();
    
    public static final Metric<Integer> SwallowedExceptionCount = new Metric.Builder("overops_swallowed_exceptions", "Swallowed Exceptions", Metric.ValueType.INT)
    .setDescription("Swallowed Exception Count")
    .setQualitative(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(OO_DOMAIN)
    .setBestValue(0.0)
    .create();

    public static final Metric<Integer> LogErrorCount = new Metric.Builder("overops_log_exceptions", "Log Errors", Metric.ValueType.INT)
    .setDescription("Log Error Count")
    .setQualitative(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(OO_DOMAIN)
    .setBestValue(0.0)
    .create();

    public static final Metric<Integer> CustomExceptionCount = new Metric.Builder("overops_custom_errors", "Custom Errors", Metric.ValueType.INT)
    .setDescription("Custom Error Count")
    .setQualitative(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(OO_DOMAIN)
    .setBestValue(0.0)
    .create();

    public static final Metric<Integer> HTTPErrors = new Metric.Builder("overops_http_errors", "HTTP Errors", Metric.ValueType.INT)
    .setDescription("HTTP Error Count")
    .setQualitative(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(OO_DOMAIN)
    .setBestValue(0.0)
    .create();

  @Override
  public List<Metric> getMetrics() {
    return asList(Total_Errors, UncaughtExceptionCount, SwallowedExceptionCount, LogErrorCount, CustomExceptionCount, HTTPErrors, CaughtExceptionCount);
  }
}
