package com.overops.plugins.sonar;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.overops.plugins.sonar.config.OverOpsRulesDefinition.*;

public class DotNetEventSensor extends AbstractEventSensor {

	private static final Logger LOGGER = Loggers.get(DotNetEventSensor.class);

	@Override
	public void describe(SensorDescriptor descriptor) {
		descriptor.name("OverOps Event Issues Sensor for .NET");
		descriptor.onlyOnLanguage(CS_LANGUAGE);
		descriptor.createIssuesForRuleRepositories(CS_REPOSITORY);
	}

	@Override
	public InputFile sourceFile(FileSystem fs, String filePath) {
		return fs.inputFile(fs.predicates().hasAbsolutePath(filePath));
	}

	public RuleKey ruleKey(){
		return CS_EVENT_RULE;
	}

}
