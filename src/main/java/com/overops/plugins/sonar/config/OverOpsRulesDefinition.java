package com.overops.plugins.sonar.config;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Sonar Rules Definition
 */
public class OverOpsRulesDefinition implements RulesDefinition {

	public static final String JAVA_REPOSITORY = "overops";
	public static final String JAVA_LANGUAGE = "java";
	public static final RuleKey JAVA_EVENT_RULE = RuleKey.of(JAVA_REPOSITORY, "event");

	public static final String CS_REPOSITORY = "overops-dotnet";
	public static final String CS_LANGUAGE = "cs";
	public static final RuleKey CS_EVENT_RULE = RuleKey.of(CS_REPOSITORY, "event");

	public static final double ARBITRARY_GAP = 1.0;
	private static final String GAP_MULTIPLIER = "10min"; // format is 2d 10h 15m

	@Override
	public void define(Context context) {
		// Java
		NewRepository javaRepository = context.createRepository(JAVA_REPOSITORY, JAVA_LANGUAGE).setName("OverOps");

		NewRule javaEventRule = javaRepository.createRule(JAVA_EVENT_RULE.rule())
			.setName("OverOps Event")
			.setHtmlDescription("This issue was detected by OverOps")
			.setTags(JAVA_REPOSITORY)
			.setStatus(RuleStatus.READY) // default
			.setSeverity(Severity.MAJOR) // default
			.setActivatedByDefault(true)
			.setType(RuleType.BUG);

		// each event costs ARBITRARY_GAP * GAP_MULTIPLIER = 10min
		javaEventRule.setDebtRemediationFunction(javaEventRule.debtRemediationFunctions().linear(GAP_MULTIPLIER));

		// finalize the definition
		javaRepository.done();

		// .NET
		NewRepository csRepository = context.createRepository(CS_REPOSITORY, CS_LANGUAGE).setName("OverOps");

		NewRule csEventRule = csRepository.createRule(CS_EVENT_RULE.rule())
			.setName("OverOps Event")
			.setHtmlDescription("This issue was detected by OverOps")
			.setTags(CS_REPOSITORY)
			.setStatus(RuleStatus.READY) // default
			.setSeverity(Severity.MAJOR) // default
			.setActivatedByDefault(true)
			.setType(RuleType.BUG);

		// each event costs ARBITRARY_GAP * GAP_MULTIPLIER = 10min
		csEventRule.setDebtRemediationFunction(csEventRule.debtRemediationFunctions().linear(GAP_MULTIPLIER));

		// finalize the definition
		csRepository.done();

	}
}