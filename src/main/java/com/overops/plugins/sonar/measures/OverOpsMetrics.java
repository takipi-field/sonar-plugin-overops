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

import java.util.*;
import java.util.regex.Pattern;

import static com.overops.plugins.sonar.measures.OverOpsQualityGateStat.*;
import static com.overops.plugins.sonar.rules.checks.OverOpsChecks.OVEROPS_ROOT_TAG;
import static com.overops.plugins.sonar.rules.checks.OverOpsChecks.REPOSITORY_KEY;
import static java.util.Arrays.asList;

public class OverOpsMetrics implements Metrics {
    public static final String MESSAGE_PATTERN_PREFIX = " has been detected [ID-";
    public static final String MESSAGE_PATTERN_SUFFIX = "]";
    public static final Pattern EXCEPTION_PATTERN = Pattern.compile(" has been detected \\[ID-(.*?)\\]");
    public static String OVER_OPS_DOMAIN = "OverOps Quality Gates";

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

    public static final Metric<Integer> NewQualityGateCount = new Metric.Builder("overops_new_quality_gate", "New Exceptions", Metric.ValueType.INT)
            .setDescription("New Exceptions")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> ResurfacedQualityGateCount = new Metric.Builder("overops_resurfaced_quality_gate", "Resurfaced Exceptions", Metric.ValueType.INT)
            .setDescription("Resurfaced Exceptions")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> CriticalQualityGateCount = new Metric.Builder("overops_critical_quality_gate", "Critical Exceptions", Metric.ValueType.INT)
            .setDescription("Critical Exceptions")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public static final Metric<Integer> IncreasingQualityGateCount = new Metric.Builder("overops_incresing_quality_gate", "Increasing Exceptions", Metric.ValueType.INT)
            .setDescription("Increasing Exceptions")
            .setQualitative(true)
            .setDirection(Metric.DIRECTION_WORST)
            .setDomain(OVER_OPS_DOMAIN)
            .setBestValue(0.0)
            .create();

    public enum OverOpsMetric {
        CAUGHT_EXCEPTION("Caught Exception", CaughtExceptionCount,
                RuleType.BUG, Severity.MAJOR, Constants.CAUGHT_EXCEPTION_RULE_KEY,
                "-caught-exception", "Caught Exception rule", new String[]{"notgate"}),
        SWALLOWED_EXCEPTION("Swallowed Exception", SwallowedExceptionCount,
                RuleType.BUG, Severity.MAJOR, Constants.SWALLOWED_EXCEPTION_RULE_KEY,
                "-swallowed-exception", "Swallowed Exception rule", new String[]{"notgate"}),
        UNCAUGHT_EXCEPTION("Uncaught Exception", UncaughtExceptionCount,
                RuleType.BUG, Severity.MAJOR, Constants.UNCAUGHT_EXCEPTION_RULE_KEY,
                "-uncaught-exception", "Uncaught Exception rule", new String[]{"notgate"}),
        LOGGED_ERROR("Logged Error", LogErrorCount,
                RuleType.BUG, Severity.MINOR, Constants.LOGGED_ERROR_RULE_KEY,
                "-logged-error", "Logged Error rule", new String[]{"notgate"}),
        CUSTOM_EVENT("Custom Event", CustomExceptionCount,
                RuleType.BUG, Severity.MAJOR, Constants.CUSTOM_EVENT_RULE_KEY,
                "-custom-event", "Custom Event rule", new String[]{"notgate"}),
        HTTP_ERROR("HTTP Error", HTTPErrors,
                RuleType.BUG, Severity.MINOR, Constants.HTTP_ERROR_RULE_KEY,
                "-http-error", "HTTP Error rule", new String[]{"notgate"}),
        CRITICAL_EXCEPTION("Critical Exception", CriticalExceptionCount,
                RuleType.BUG, Severity.MAJOR, Constants.CRITICAL_EXCEPTION_RULE_KEY,
                "-critical-exception", "Critical Exception rule", new String[]{"notgate"}),
        NEW_QUALITY_GATE("NEW_QUALITY_GATE", NewQualityGateCount,
                RuleType.VULNERABILITY, Severity.MAJOR, Constants.NEW_QUALITY_GATE_KEY,
                "-new", "NEW QUALITY GATE", new String[]{NEW_ERRORS_QUALITY_GATE}),
        CRITICAL_QUALITY_GATE("CRITICAL_QUALITY_GATE", CriticalQualityGateCount,
                RuleType.VULNERABILITY, Severity.MAJOR, Constants.CRITICAL_QUALITY_GATE,
                "-critical", "CRITICAL QUALITY GATE", new String[]{CRITICAL_ERROR_QUALITY_GATE}),
        RESURFACED_QUALITY_GATE("RESURFACED_QUALITY_GATE", ResurfacedQualityGateCount,
                RuleType.VULNERABILITY, Severity.MAJOR, Constants.RESURFACED_QUALITY_GATE_KEY,
                "-resurfaced", "RESURFACED QUALITY GATE", new String[]{RESURFACED_ERROR_QUALITY_GATE}),
        INCREASING_QUALITY_GATE("INCREASING_QUALITY_GATE", IncreasingQualityGateCount,
                RuleType.VULNERABILITY, Severity.MAJOR, Constants.INCREASING_QUALITY_GATE_KEY,
                "-increasing", "INCREASING QUALITY GATE", new String[]{INCREASING_ERRORS_QUALITY_GATE});

        public final String overOpsType;
        public final Metric metric;
        public final RuleType ruleType;
        public final Severity severity;
        public final String ruleKey;
        public final String ruleFullKey;
        public String[] ruleTags;
        public final String ruleTitle;
        public final Set<String> qualityGate;

        OverOpsMetric(String overOpsType,
                      Metric metric,
                      RuleType ruleType,
                      Severity severity,
                      String ruleKey,
                      String ruleTagEnding,
                      String ruleTitle, String[] qualityGateArray) {
            this(overOpsType, metric, ruleType, severity, ruleKey, ruleTitle, qualityGateArray);
            this.ruleTags = new String[]{OVEROPS_ROOT_TAG, OVEROPS_ROOT_TAG + ruleTagEnding};
        }

        OverOpsMetric(String overOpsType,
                      Metric metric,
                      RuleType ruleType,
                      Severity severity,
                      String ruleKey,
                      String[] ruleTagsEnding,
                      String ruleTitle, String[] qualityGateArray) {
            this(overOpsType, metric, ruleType, severity, ruleKey, ruleTitle, qualityGateArray);
            List<String> tags = new ArrayList<>();
            tags.add(OVEROPS_ROOT_TAG);
            for (String tagEnding: ruleTagsEnding) {
                tags.add(OVEROPS_ROOT_TAG + tagEnding);
            }
            this.ruleTags = tags.toArray(new String[tags.size()]);
        }

        OverOpsMetric(String overOpsType, Metric metric, RuleType ruleType, Severity severity, String ruleKey, String ruleTitle, String[] qualityGateArray) {
            this.overOpsType = overOpsType;
            this.metric = metric;
            this.ruleType = ruleType;
            this.severity = severity;
            this.ruleKey = ruleKey;
            this.ruleFullKey = REPOSITORY_KEY + ":" + ruleKey;
            this.ruleTitle = ruleTitle;
            this.qualityGate = new TreeSet<>(Arrays.asList(qualityGateArray));
        }

        public static Metric getMetric(String overOpsType) {
            for (OverOpsMetric overOpsMetric : values()) {
                if (overOpsMetric.overOpsType.equals(overOpsType)) {
                    return overOpsMetric.metric;
                }
            }

            return null;
        }

        public static Metric getMetricByQualityGate(String qualityGateKey) {
            for (OverOpsMetric overOpsMetric : values()) {
                String key = String.join(".", overOpsMetric.qualityGate);
                if (key.equals(qualityGateKey)) {
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
            public static final String HTTP_ERROR_RULE_KEY = "HTTPError";
            public static final String CRITICAL_EXCEPTION_RULE_KEY = "CriticalException";
            public static final String NEW_QUALITY_GATE_KEY = "NewQualityGate";
            public static final String CRITICAL_QUALITY_GATE = "CriticalQualityGate";
            public static final String RESURFACED_QUALITY_GATE_KEY = "ResurfacedQualityGate";
            public static final String INCREASING_QUALITY_GATE_KEY = "IncreasingQualityGate";
        }
    }

    @Override
    public List<Metric> getMetrics() {
        return getMetricsList();
    }

    public static List<Metric> getMetricsList() {
        //return asList(UncaughtExceptionCount, SwallowedExceptionCount, LogErrorCount, CustomExceptionCount, HTTPErrors, CaughtExceptionCount, CriticalExceptionCount, NewQualityGateCount);
        return asList(NewQualityGateCount, IncreasingQualityGateCount, ResurfacedQualityGateCount, CriticalExceptionCount);

    }

}
