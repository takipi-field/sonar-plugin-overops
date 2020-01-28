package com.overops.plugins.sonar;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

public class JavaRulesDefinition implements RulesDefinition {
	public static final String REPOSITORY = "overops";
	public static final String JAVA_LANGUAGE = "java";
	public static final RuleKey EVENT_RULE = RuleKey.of(REPOSITORY, "event");

	public static final double ARBITRARY_GAP = 1.0;

	@Override
	public void define(Context context) {
		NewRepository repository = context.createRepository(REPOSITORY, JAVA_LANGUAGE).setName("OverOps");

		// NewRule eventRule = 
		repository.createRule(EVENT_RULE.rule())
			.setName("OverOps Event")
			.setHtmlDescription("This issue was detected by OverOps")
			.setTags("overops")
			.setStatus(RuleStatus.READY) // default
			.setSeverity(Severity.MAJOR) // default
			.setActivatedByDefault(true)
			.setType(RuleType.BUG);

		// finalize the definition
		repository.done();

	}
}