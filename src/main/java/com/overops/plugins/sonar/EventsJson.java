package com.overops.plugins.sonar;

import java.util.List;

public class EventsJson {
	private String rule;
	private String componentKey; 
	private List<IssueComment> issues;

	public void setRule(String rule) {
		this.rule = rule;
	}
	public String getRule() {
		return rule;
	}
	public void setComponentKey(String componentKey) {
		this.componentKey = componentKey;
	}
	public String getComponentKey() {
		return componentKey;
	}
	public void setIssues(List<IssueComment> issues) {
		this.issues = issues;
	}
	public List<IssueComment> getIssues() {
		return issues;
	}

}

/*
String rule = "overops:event";
String componentKey = context.project().key() + sourcefile;

ArrayList<IssueComment> issues = new ArrayList<IssueComment>(int size);

*/