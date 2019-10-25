package com.overops.plugins.sonar.rules;

import com.google.gson.Gson;
import com.overops.plugins.sonar.rules.checks.DefaultChecks;
import com.overops.plugins.sonar.rules.checks.OverOpsCaughtExceptionCheck;
import com.overops.plugins.sonar.rules.checks.OverOpsUncaughtExceptionCheck;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.squidbridge.annotations.RuleTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

public class RuleDefinitionImplementation implements RulesDefinition, CheckRegistrar {
    private static final String RESOURCE_BASE_PATH = "resources/org/sonar/l10n/rules";
    private static final Logger LOGGER = Loggers.get(RuleDefinitionImplementation.class);
    private static final String JAVA_LANGUAGE = "java";
    private final Gson gson = new Gson();

    @Override
    public void register(RegistrarContext registrarContext) {
        LOGGER.info("Adding register ");
        registrarContext.registerClassesForRepository(DefaultChecks.REPOSITORY_KEY,
            Arrays.asList(OverOpsCaughtExceptionCheck.class, OverOpsUncaughtExceptionCheck.class), Collections.EMPTY_LIST);
    }


    @Override
    public void define(Context context) {
        LOGGER.info("Adding custom OverOps rules");
        NewRepository repository = context.createRepository(DefaultChecks.REPOSITORY_KEY, JAVA_LANGUAGE).setName("OverOps analyzer");
        //test(repository);
        for (Class<? extends JavaCheck> check : DefaultChecks.getChecks()) {
            new RulesDefinitionAnnotationLoader().load(repository, new Class[]{check});
            newRule(check, repository);
        }
        repository.done();
    }

    protected void newRule(Class<? extends JavaCheck> ruleClass, NewRepository repository) {

        org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.check.Rule.class);
        if (ruleAnnotation == null) {
            throw new IllegalArgumentException("No Rule annotation was found on " + ruleClass);
        }
        String ruleKey = ruleAnnotation.key();
        if (StringUtils.isEmpty(ruleKey)) {
            throw new IllegalArgumentException("No key is defined in Rule annotation of " + ruleClass);
        }
        NewRule rule = repository.rule(ruleKey);
        if (rule == null) {
            throw new IllegalStateException("No rule was created for " + ruleClass + " in " + repository.key());
        }
        ruleMetadata(rule);

        rule.setTemplate(AnnotationUtils.getAnnotation(ruleClass, RuleTemplate.class) != null);
    }

    private void ruleMetadata(NewRule rule) {
        String metadataKey = rule.key();
        addHtmlDescription(rule, metadataKey);
        addMetadata(rule, metadataKey);
    }

    private void addMetadata(NewRule rule, String metadataKey) {
        URL resource = RuleDefinitionImplementation.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.json");
        if (resource != null) {
            RuleMetatada metatada = gson.fromJson(readResource(resource), RuleMetatada.class);
            rule.setSeverity(metatada.defaultSeverity.toUpperCase(Locale.US));
            rule.setName(metatada.title);
            rule.addTags(metatada.tags);
            rule.setType(RuleType.valueOf(metatada.type));
            rule.setStatus(RuleStatus.valueOf(metatada.status.toUpperCase(Locale.US)));
            if (metatada.remediation != null) {
                rule.setDebtRemediationFunction(metatada.remediation.remediationFunction(rule.debtRemediationFunctions()));
                rule.setGapDescription(metatada.remediation.linearDesc);
            }
        }
    }

    private static void addHtmlDescription(NewRule rule, String metadataKey) {
        LOGGER.info("addHtmlDescription resource search  " + RESOURCE_BASE_PATH + "/" + metadataKey + "_java.html");
        URL resource = RuleDefinitionImplementation.class.getResource(RESOURCE_BASE_PATH + "/" + metadataKey + "_java.html");
        LOGGER.info("addHtmlDescription resource " +(resource == null ? "not found" : resource.getPath()));
        if (resource != null) {
            rule.setHtmlDescription(readResource(resource));
        }
    }

    private static String readResource(URL resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read: " + resource, e);
        }
    }

    private static class RuleMetatada {
        String title;
        String status;

        Remediation remediation;

        String type;
        String[] tags;
        String defaultSeverity;
    }

    private static class Remediation {
        String func;
        String constantCost;
        String linearDesc;
        String linearOffset;
        String linearFactor;

        public DebtRemediationFunction remediationFunction(DebtRemediationFunctions drf) {
            if (func.startsWith("Constant")) {
                return drf.constantPerIssue(constantCost.replace("mn", "min"));
            }
            if ("Linear".equals(func)) {
                return drf.linear(linearFactor.replace("mn", "min"));
            }
            return drf.linearWithOffset(linearFactor.replace("mn", "min"), linearOffset.replace("mn", "min"));
        }
    }

    private void test(NewRepository repository) {
        NewRule x1Rule = repository.createRule("x12")
                .setName("x12")
                .addTags("some-tg")
                .setHtmlDescription("Generate an issue on empty lines")
                .setInternalKey("x12")
                //.setScope(RuleScope.ALL)
                // optional status. Default value is READY.
                .setStatus(RuleStatus.BETA)
                // default severity when the rule is activated on a Quality profile. Default value is MAJOR.
                .setSeverity(Severity.MINOR);

        // optional type for SonarQube Quality Model. Default is RulesDefinition.Type.CODE_SMELL.
        //.setType(RulesDefinition.Type.BUG)
        x1Rule.setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));
        x1Rule.createParam("acceptWhitespace")
                .setDefaultValue("false")
                .setType(RuleParamType.BOOLEAN)
                .setDescription("Accept whitespaces on the line");
        // don't forget to call done() to finalize the definition
    }
}