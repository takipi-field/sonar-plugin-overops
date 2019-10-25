package com.overops.plugins.sonar.rest;

import java.util.List;

public class SQIssue {
    public String key;
    public String rule;
    public String severity;
    public String component;
    public int line;
    public String status;
    public String message;
    public List<String> tags;
    public String creationDate;
    public String updateDate;
    public String type;
    public List<SQComment> comments;
}
