package com.overops.plugins.sonar.config;

import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class OverOpsRulesDefinitionTest {

    @Test
    void execute(){
        OverOpsRulesDefinition definition = new OverOpsRulesDefinition();
        RulesDefinition.Context context = new RulesDefinition.Context();

        // Execute Test Method
        definition.define(context);

        // Assert Java Rule Created
        RulesDefinition.Repository javaRepo = context.repository(OverOpsRulesDefinition.JAVA_REPOSITORY);
        RulesDefinition.Rule javaRule = javaRepo.rule(OverOpsRulesDefinition.JAVA_EVENT_RULE.rule());

        assertThat(javaRule.name()).isEqualTo("OverOps Event");
        assertThat(javaRule.htmlDescription()).isEqualTo("This issue was detected by OverOps");
        assertThat(javaRule.tags()).contains(OverOpsRulesDefinition.JAVA_REPOSITORY);
        assertThat(javaRule.status()).isEqualTo(RuleStatus.READY);
        assertThat(javaRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(javaRule.activatedByDefault()).isTrue();
        assertThat(javaRule.type()).isEqualTo(RuleType.BUG);

        // Assert CS Rule Created
        RulesDefinition.Repository csRepo = context.repository(OverOpsRulesDefinition.CS_REPOSITORY);
        RulesDefinition.Rule csRule = csRepo.rule(OverOpsRulesDefinition.CS_EVENT_RULE.rule());

        assertThat(csRule.name()).isEqualTo("OverOps Event");
        assertThat(csRule.htmlDescription()).isEqualTo("This issue was detected by OverOps");
        assertThat(csRule.tags()).contains(OverOpsRulesDefinition.CS_REPOSITORY);
        assertThat(csRule.status()).isEqualTo(RuleStatus.READY);
        assertThat(csRule.severity()).isEqualTo(Severity.MAJOR);
        assertThat(csRule.activatedByDefault()).isTrue();
        assertThat(csRule.type()).isEqualTo(RuleType.BUG);
    }
}
