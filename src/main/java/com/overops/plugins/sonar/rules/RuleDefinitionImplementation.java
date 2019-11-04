package com.overops.plugins.sonar.rules;

import com.google.gson.Gson;
import com.overops.plugins.sonar.measures.OverOpsMetrics;
import com.overops.plugins.sonar.rules.checks.OverOpsChecks;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.stream.Collectors;

public class RuleDefinitionImplementation implements RulesDefinition, CheckRegistrar {
    private static final Logger LOGGER = Loggers.get(RuleDefinitionImplementation.class);
    private static final String RESOURCE_BASE_PATH = "/org/sonar/l10n/rules";
    private static final String OVER_OPS_REPOSITORY_NAME = "OverOps analyzer";
    private static final String JAVA_LANGUAGE = "java";

    @Override
    public void register(RegistrarContext registrarContext) {
        registrarContext.registerClassesForRepository(OverOpsChecks.REPOSITORY_KEY, OverOpsChecks.getChecks(), Collections.EMPTY_LIST);
    }

    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(OverOpsChecks.REPOSITORY_KEY, JAVA_LANGUAGE).setName(OVER_OPS_REPOSITORY_NAME);
        for (Class<? extends JavaCheck> check : OverOpsChecks.getChecks()) {
            new RulesDefinitionAnnotationLoader().load(repository, new Class[]{check});
            newRule(check, repository);
        }
        repository.done();
    }

    protected void newRule(Class<? extends JavaCheck> ruleClass, NewRepository repository) {
        org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.check.Rule.class);
        if (ruleAnnotation == null) {
            LOGGER.error("No Rule annotation was found on " + ruleClass);
            return;
        }
        String ruleKey = ruleAnnotation.key();
        if (StringUtils.isEmpty(ruleKey)) {
            LOGGER.error("No key is defined in Rule annotation of " + ruleClass);
            return;
        }
        NewRule rule = repository.rule(ruleKey);
        if (rule == null) {
            LOGGER.error("No rule was created for " + ruleClass + " in " + repository.key());
            return;
        }
        ruleMetadata(rule);
    }

    private void ruleMetadata(NewRule rule) {
        String ruleKey = rule.key();
        addHtmlDescription(rule, ruleKey);
        addMetadata(rule, ruleKey);
    }

    private void addMetadata(NewRule rule, String ruleKey) {
        OverOpsMetrics.OverOpsMetric ooMetrics = OverOpsMetrics.OverOpsMetric.getByRuleKey(ruleKey);
        if (ooMetrics != null) {
            rule.setSeverity(ooMetrics.severity.toString());
            rule.setName(ooMetrics.ruleTitle);
            rule.addTags(ooMetrics.ruleTags);
            rule.setType(ooMetrics.ruleType);
            rule.setStatus(RuleStatus.READY);

            // TODO find what is proper remediation
//            if (metaDada.remediation != null) {
//                rule.setDebtRemediationFunction(metaDada.remediation.remediationFunction(rule.debtRemediationFunctions()));
//                rule.setGapDescription(metaDada.remediation.linearDesc);
//            }
        }
    }

    private static void addHtmlDescription(NewRule rule, String ruleKey) {
        URL resource = RuleDefinitionImplementation.class.getResource(RESOURCE_BASE_PATH + "/" + ruleKey + "_java.html");
        if (resource != null) {
            rule.setHtmlDescription(readResource(resource));
        }
    }

    private static String readResource(URL resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            LOGGER.error("Failed to read: " + resource, e);
        }

        return null;
    }
}