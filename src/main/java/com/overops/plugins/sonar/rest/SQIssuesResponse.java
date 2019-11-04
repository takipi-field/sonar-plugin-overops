package com.overops.plugins.sonar.rest;
import com.takipi.api.core.result.intf.ApiResult;

import java.util.List;

public class SQIssuesResponse implements ApiResult {
    public int total;
    public List<SQIssue> issues;
}
