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
    public static final Pattern EXCEPTION_PATTERN = Pattern.compile(" \\[ID-(.*?)\\]");
    public static String OVER_OPS_DOMAIN = "OverOps Quality Gates";

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
        NEW_QG_METRIC("NEW_QG", NewQualityGateCount,
                RuleType.BUG, Severity.MAJOR, Constants.NEW_QG_KEY,
                "-new", "NEW QUALITY GATE", new String[]{NEW_QG_MARKER}),
        CRITICAL_QG_METRIC("CRITICAL_QG", CriticalQualityGateCount,
                RuleType.BUG, Severity.CRITICAL, Constants.CRITICAL_QG_KEY,
                "-critical", "CRITICAL QUALITY GATE", new String[]{CRITICAL_QG_MARKER}),
        RESURFACED_QG_METRIC("RESURFACED_QG", ResurfacedQualityGateCount,
                RuleType.BUG, Severity.MAJOR, Constants.RESURFACED_QG_KEY,
                "-resurfaced", "RESURFACED QUALITY GATE", new String[]{RESURFACED_QG_MARKER}),
        INCREASING_QG_METRIC("INCREASING_QG", IncreasingQualityGateCount,
                RuleType.BUG, Severity.MINOR, Constants.INCREASING_QG_KEY,
                "-increasing", "INCREASING QUALITY GATE", new String[]{INCREASING_QG_MARKER}),
        NEW_CRITICAL_QG_METRIC("NEW_CRITICAL_QG", null,
                RuleType.BUG, Severity.CRITICAL, Constants.NEW_CRITICAL_QG_KEY,
                new String[]{"-new", "-critical"}, "NEW AND CRITICAL QUALITY GATE",
                new String[]{NEW_QG_MARKER, CRITICAL_QG_MARKER}),
        RESURFACED_CRITICAL_QG_METRIC("RESURFACED_CRITICAL_QG", null,
                RuleType.BUG, Severity.CRITICAL, Constants.RESURFACED_CRITICAL_QG_KEY,
                new String[]{"-resurfaced", "-critical"}, "RESURFACED AND CRITICAL QUALITY GATE",
                new String[]{RESURFACED_QG_MARKER, CRITICAL_QG_MARKER}),
        INCREASING_CRITICAL_QG_METRIC("INCREASING_CRITICAL_QG", null,
                RuleType.BUG, Severity.CRITICAL, Constants.INCREASING_CRITICAL_QG_KEY,
                new String[]{"-increasing", "-critical"}, "INCREASING AND CRITICAL QUALITY GATE",
                new String[]{INCREASING_QG_MARKER, CRITICAL_QG_MARKER});

        public final String overOpsType;
        public final Metric<Integer> metric;
        public final RuleType ruleType;
        public final Severity severity;
        public final String ruleKey;
        public final String ruleFullKey;
        public String[] ruleTags;
        public final String ruleTitle;
        public final Set<String> qualityGate;
        public final String qualityGateKey;

        OverOpsMetric(String overOpsType,
                      Metric<Integer> metric,
                      RuleType ruleType,
                      Severity severity,
                      String ruleKey,
                      String ruleTagEnding,
                      String ruleTitle, String[] qualityGateArray) {
            this(overOpsType, metric, ruleType, severity, ruleKey, ruleTitle, qualityGateArray);
            this.ruleTags = new String[]{OVEROPS_ROOT_TAG, OVEROPS_ROOT_TAG + ruleTagEnding};
        }

        OverOpsMetric(String overOpsType,
                      Metric<Integer> metric,
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

        OverOpsMetric(String overOpsType, Metric<Integer> metric, RuleType ruleType, Severity severity, String ruleKey, String ruleTitle, String[] qualityGateArray) {
            this.overOpsType = overOpsType;
            this.metric = metric;
            this.ruleType = ruleType;
            this.severity = severity;
            this.ruleKey = ruleKey;
            this.ruleFullKey = REPOSITORY_KEY + ":" + ruleKey;
            this.ruleTitle = ruleTitle;
            this.qualityGate = new TreeSet<>(Arrays.asList(qualityGateArray));
            this.qualityGateKey = getKey(qualityGate);
        }

        public static Metric<Integer> getMetric(String overOpsType) {
            for (OverOpsMetric overOpsMetric : values()) {
                if (overOpsMetric.overOpsType.equals(overOpsType)) {
                    return overOpsMetric.metric;
                }
            }

            return null;
        }


        public static Metric<Integer> getMetricByQualityGate(String qualityGateKey) {
            for (OverOpsMetric overOpsMetric : values()) {
                if (overOpsMetric.qualityGateKey.equals(qualityGateKey)) {
                    return overOpsMetric.metric;
                }
            }

            return null;
        }

        public static OverOpsMetric getOverOpsByQualityGate(String qualityGateKey) {
            for (OverOpsMetric overOpsMetric : values()) {
                if (overOpsMetric.qualityGateKey.equals(qualityGateKey)) {
                    return overOpsMetric;
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

        public boolean isCombo() {
            return qualityGate.size() > 1;
        }

        public static class Constants {
            public static final String NEW_QG_KEY = "NewQualityGate";
            public static final String CRITICAL_QG_KEY = "CriticalQualityGate";
            public static final String RESURFACED_QG_KEY = "ResurfacedQualityGate";
            public static final String INCREASING_QG_KEY = "IncreasingQualityGate";
            public static final String NEW_CRITICAL_QG_KEY = "NewCriticalQualityGate";
            public static final String RESURFACED_CRITICAL_QG_KEY = "ResurfacedCriticalQualityGate";
            public static final String INCREASING_CRITICAL_QG_KEY = "IncreasingCriticalQualityGate";
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Metric> getMetrics() {
        return asList(NewQualityGateCount, IncreasingQualityGateCount, ResurfacedQualityGateCount, CriticalQualityGateCount);
    }
}
