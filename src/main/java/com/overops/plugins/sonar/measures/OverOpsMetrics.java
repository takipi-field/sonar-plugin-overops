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

import org.sonar.api.batch.rule.Severity;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.rules.RuleType;

import java.util.List;

import static com.overops.plugins.sonar.rules.checks.OverOpsChecks.OVEROPS_ROOT_TAG;
import static com.overops.plugins.sonar.rules.checks.OverOpsChecks.REPOSITORY_KEY;
import static java.util.Arrays.asList;

public class OverOpsMetrics implements Metrics {
    public static String OVER_OPS_DOMAIN = REPOSITORY_KEY;

    public static final Metric<Integer> CaughtExceptionCount = new Metric.Builder("overops_caught_exception", "Caught Exceptions", Metric.ValueType.INT)
            .setDescription("Caught Exception Count")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> UncaughtExceptionCount = new Metric.Builder("overops_uncaught_exceptions", "Uncaught Exceptions", Metric.ValueType.INT)
            .setDescription("Number of Uncaught Exception")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> SwallowedExceptionCount = new Metric.Builder("overops_swallowed_exceptions", "Swallowed Exceptions", Metric.ValueType.INT)
            .setDescription("Swallowed Exception Count")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> LogErrorCount = new Metric.Builder("overops_log_exceptions", "Log Errors", Metric.ValueType.INT)
            .setDescription("Log Error Count")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> CustomExceptionCount = new Metric.Builder("overops_custom_errors", "Custom Errors", Metric.ValueType.INT)
            .setDescription("Custom Error Count")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> HTTPErrors = new Metric.Builder("overops_http_errors", "HTTP Errors", Metric.ValueType.INT)
            .setDescription("HTTP Error Count")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> CriticalExceptionCount = new Metric.Builder("overops_critical_exception", "Critical Exception", Metric.ValueType.INT)
            .setDescription("Critical Exception Count")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public enum OverOpsMetric {
        CAUGHT_EXCEPTION("Caught Exception", CaughtExceptionCount,
                RuleType.BUG, Severity.MAJOR,
                "CaughtException", Constants.CAUGHT_EXCEPTION_RULE_KEY,
                "-caught-exception", "Caught Exception rule"),
        SWALLOWED_EXCEPTION("Swallowed Exception", SwallowedExceptionCount,
                RuleType.BUG, Severity.MAJOR,
                "", Constants.SWALLOWED_EXCEPTION_RULE_KEY,
                "", ""),
        UNCAUGHT_EXCEPTION("Uncaught Exception", UncaughtExceptionCount,
                RuleType.BUG, Severity.MAJOR,
                "UncaughtException", Constants.UNCAUGHT_EXCEPTION_RULE_KEY,
                "-uncaught-exception", "Uncaught Exception rule"),
        LOGGED_ERROR("Logged Error", LogErrorCount,
                RuleType.BUG, Severity.MINOR,
                "", Constants.LOGGED_ERROR_RULE_KEY,
                "", ""),
        CUSTOM_EVENT("Custom Event", CustomExceptionCount,
                RuleType.BUG, Severity.MAJOR,
                "", Constants.CUSTOM_EVENT_RULE_KEY,
                "", ""),
        HTTP_ERROR("HTTP Error", HTTPErrors,
                RuleType.BUG, Severity.MINOR,
                "", Constants.HTTP_ERROR_RULE_KEY,
                "", ""),
        CRITICAL_EXCEPTION("Critical Exception", CriticalExceptionCount,
                RuleType.BUG, Severity.MAJOR,
                "", Constants.CRITICAL_EXCEPTION_RULE_KEY,
                "", "");

        public final String overOpsType;
        public final Metric metric;
        public final RuleType ruleType;
        public final Severity severity;
        public final String patterName;
        public final String ruleKey;
        public final String ruleFullKey;
        public final String[] ruleTags;
        public final String ruleTitle;

        OverOpsMetric(String overOpsType,
                      Metric metric,
                      RuleType ruleType,
                      Severity severity,
                      String patterName,
                      String ruleKey,
                      String ruleTagEnding,
                      String ruleTitle) {
            this.overOpsType = overOpsType;
            this.metric = metric;
            this.ruleType = ruleType;
            this.severity = severity;
            this.patterName = patterName;
            this.ruleKey = ruleKey;
            this.ruleFullKey = REPOSITORY_KEY + ":" + ruleKey;
            this.ruleTags = new String[]{OVEROPS_ROOT_TAG, OVEROPS_ROOT_TAG + ruleTagEnding};
            this.ruleTitle = ruleTitle;
        }

        public static Metric getMetric(String overOpsType) {
            for (OverOpsMetric overOpsMetric : values()) {
                if (overOpsMetric.overOpsType.equals(overOpsType)) {
                    return overOpsMetric.metric;
                }
            }

            return null;
        }

        public static OverOpsMetric getByRuleKey(String ruleKey) {
            for (OverOpsMetric overOpsMetric : values()) {
                if (overOpsMetric.ruleKey.equals(ruleKey)) {
                    return overOpsMetric;
                }
            }

            return null;
        }

        public static OverOpsMetric getOverOpsMetric(String overOpsType) {
            for (OverOpsMetric overOpsMetric : values()) {
                if (overOpsMetric.overOpsType.equals(overOpsType)) {
                    return overOpsMetric;
                }
            }

            return null;
        }

        public static class Constants {
            public static final String CAUGHT_EXCEPTION_RULE_KEY = "CaughtException";
            public static final String SWALLOWED_EXCEPTION_RULE_KEY = "SwallowedException";
            public static final String UNCAUGHT_EXCEPTION_RULE_KEY = "UncaughtException";
            public static final String LOGGED_ERROR_RULE_KEY = "LoggedError";
            public static final String CUSTOM_EVENT_RULE_KEY = "CustomEvent";
            public static final String HTTP_ERROR_RULE_KEY = "HTTP Error";
            public static final String CRITICAL_EXCEPTION_RULE_KEY = "CriticalException";
        }
    }

    @Override
    public List<Metric> getMetrics() {
        return getMetricsList();
    }

    public static List<Metric> getMetricsList() {
        return asList(UncaughtExceptionCount, SwallowedExceptionCount, LogErrorCount, CustomExceptionCount, HTTPErrors, CaughtExceptionCount, CriticalExceptionCount);
    }

}
