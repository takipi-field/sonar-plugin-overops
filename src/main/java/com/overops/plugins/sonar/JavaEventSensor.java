package com.overops.plugins.sonar;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.rule.RuleKey;

import static com.overops.plugins.sonar.config.OverOpsRulesDefinition.*;

public class JavaEventSensor extends AbstractEventSensor {

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("OverOps Event Issues Sensor for Java");
		descriptor.onlyOnLanguage(JAVA_LANGUAGE);
		descriptor.createIssuesForRuleRepositories(JAVA_REPOSITORY);
	}

	@Override
	public InputFile sourceFile(FileSystem fs, String filePath){
		// get file matching this filePath (e.g. **/com/example/path/ClassName.java)
		return fs.inputFile(
				fs.predicates().and(
						fs.predicates().matchesPathPattern(filePath),
						fs.predicates().hasLanguage("java")
				)
		);
	}

	public RuleKey ruleKey(){
		return JAVA_EVENT_RULE;
	}

}
