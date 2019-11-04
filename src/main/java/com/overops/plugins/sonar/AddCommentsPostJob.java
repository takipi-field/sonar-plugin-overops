package com.overops.plugins.sonar;

import com.google.common.net.HttpHeaders;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.overops.plugins.sonar.measures.OverOpsEventsStatistic;
import com.overops.plugins.sonar.measures.OverOpsMetrics;
import com.overops.plugins.sonar.rest.*;
import com.overops.plugins.sonar.util.SimpleUrlClient;
import com.overops.plugins.sonar.util.TextBuilder;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.util.event.EventUtil;
import com.takipi.api.core.url.UrlClient;
import com.takipi.common.util.Pair;
import org.codehaus.plexus.util.StringUtils;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.overops.plugins.sonar.OverOpsPlugin.*;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.*;

public class AddCommentsPostJob implements PostJob {
    private static final Gson GSON;

    static {
        GSON = (new GsonBuilder()).setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    private static final Logger log = Loggers.get(AddCommentsPostJob.class);

    @Override
    public void describe(PostJobDescriptor postJobDescriptor) {
    }

    @Override
    public void execute(PostJobContext postJobContext) {
        waitForSonarQubeCreatIssuesInDb();

        Collection<OverOpsEventsStatistic.ClassStat> statistic = overOpsEventsStatistic.getStatistic();
        for (OverOpsEventsStatistic.ClassStat classStat : statistic) {
            log.info("    " +classStat.fileName);
            for (String exName : classStat.typeToEventStat.keySet()) {
                log.info("           " + exName);
            }
        }

        OverOpsMetrics.OverOpsMetric[] values = OverOpsMetrics.OverOpsMetric.values();
        for (OverOpsMetrics.OverOpsMetric metric : values) {
            addCommentsToIssuesPerRule(metric);
        }
    }

    private void waitForSonarQubeCreatIssuesInDb() {
        //This is an empirical workaround to be in phase when all created by OverOps plugin issues were available through WEB API
        try {
            Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void addCommentsToIssuesPerRule(OverOpsMetrics.OverOpsMetric metric) {
        String ruleType = metric.ruleFullKey;
        String severityType = metric.severity.toString().toUpperCase();

        if (StringUtils.isEmpty(metric.ruleTitle)) {
            log.error("Rule should not be empty, metric will be postponed " + metric.overOpsType);
            return;
        }

        SQSimpleRequest issuesPerRuleRequest = SQSimpleRequest.newBuilder()
                .setUrl(SONAR_HOST_URL + "/api/issues/search")
                .addQueryParam("rules", ruleType)
                .build();
        UrlClient.Response<String> stringResponse = SimpleUrlClient.newBuilder().build().get(issuesPerRuleRequest);

        SQIssuesResponse sqIssuesResponse = GSON.fromJson(stringResponse.data, SQIssuesResponse.class);
        for (SQIssue sqIssue :sqIssuesResponse.issues) {
            if ((sqIssue.status == null) ||
                    (sqIssue.status.indexOf("OPEN") == -1)) {
                continue;
            }
            Matcher matcher = EXCEPTION_PATTERN.matcher(sqIssue.message);
            boolean matched = matcher.find();
            if (matched) {
                String issueEventId = matcher.group(1);
                EventResult eventResult = getEventDataById(issueEventId);
                if (eventResult != null && !isOverOpsCommentAdded(sqIssue, eventResult, severityType)) {
                    addCommentToSonarQubeIssue(sqIssue, issueEventId, eventResult);
                }
            }
        }
    }

    private boolean isOverOpsCommentAdded(SQIssue sqIssue, EventResult eventResult, String severityType) {
        SQIssueFullData fullData = getComments(sqIssue, severityType);
        if (fullData != null && fullData.issue != null && fullData.issue.comments != null) {
            Pattern overOpsMessagePattern = Pattern.compile("Drill down into");
            Optional<SQComment> comment = fullData.issue.comments.stream().filter(sqComment -> {
                if (sqComment.markdown != null) {
                    return overOpsMessagePattern.matcher(sqComment.markdown).find();
                }
                return false;
            }).findAny();
            return comment.isPresent();
        }
        return false;
    }

    private SQIssueFullData getComments(SQIssue sqIssue, String severityType) {
        SQSimpleRequest request = SQSimpleRequest.newBuilder()
                .setUrl(SONAR_HOST_URL + "/api/issues/set_severity")
                .addQueryParam("issue", sqIssue.key)
                .addQueryParam("severity", severityType)
                .build();
        UrlClient.Response<String> stringResponse = SimpleUrlClient
                .newBuilder()
                .setAuth(getSonarQubeAuth())
                .build().post(request);

        if (stringResponse != null) {
            return GSON.fromJson(stringResponse.data, SQIssueFullData.class);
        }

        return null;
    }

    private Pair<String, String> getSonarQubeAuth() {
        return Pair.of(HttpHeaders.AUTHORIZATION, "Basic " + AUTH_DATA);
    }

    private void addCommentToSonarQubeIssue(SQIssue sqIssue, String issueEventId, EventResult eventResult) {
        String description = getDescription(issueEventId, eventResult);
        try {
            SQSimpleRequest addComment = SQSimpleRequest.newBuilder()
                    .setUrl(SONAR_HOST_URL + "/api/issues/add_comment")
                    .addQueryParam("issue", sqIssue.key)
                    .addQueryParam("text", URLEncoder.encode(description, StandardCharsets.UTF_8.toString()))
                    .build();
            SimpleUrlClient.newBuilder()
                    .setAuth(Pair.of(HttpHeaders.AUTHORIZATION,"Basic " + AUTH_DATA))
                    .build().post(addComment);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private String getDescription(String issueEventId, EventResult eventResult) {
        String stackTrace = new TextBuilder().addArray(eventResult.stack_frames, "> at ").build();

        return "*Drill down into* " +
                "[Event Analysis]("+ getARCLinkForEvent(issueEventId) + ")" + "\n" +
                "*Stack trace:*\n" +
                stackTrace;
    }

    private EventResult getEventDataById(String issueEventId) {
        Optional<EventResult> first = OverOpsPlugin.volumeResult.events.stream().filter(eventResult -> eventResult.id.equals(issueEventId)).findFirst();
        return first.isPresent() ? first.get() : null;
    }

    private String getARCLinkForEvent(String eventId) {
        String arcLink = null;
        try { arcLink = EventUtil.getEventRecentLinkDefault(apiClient, environmentKey, eventId, from, to,
                Arrays.asList(applicationName), Arrays.asList(), Arrays.asList(deploymentName),
                (int) (1440 * daysSpan)


        );
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (arcLink == null) {
            arcLink = " WE detect null arc EventUtil.getEventRecentLinkDefault( apiClient, \"" +
                    "\" ,  \"" + environmentKey +
                    "\" ,  \"" + eventId +
                    "\" ,  \"" + from.toString(formatter) +
                    "\" ,  \"" + to.toString(formatter) +
                    "\" , Arrays.asList(\"" + applicationName +
                    "\"), Arrays.asList(),  Arrays.asList(\"" + deploymentName + "\")" +
                    ", " + String.valueOf((int) (1440 * daysSpan)) + " )";
            log.info(arcLink);
        }

        return arcLink;
    }
}
