package com.overops.plugins.sonar;

import com.google.common.net.HttpHeaders;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.overops.plugins.sonar.measures.EventsStatistic;
import com.overops.plugins.sonar.measures.OverOpsMetrics;
import com.overops.plugins.sonar.rest.*;
import com.overops.plugins.sonar.util.SimpleUrlClient;
import com.overops.plugins.sonar.util.TextBuilder;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
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

public class AddCommentsPostJob implements PostJob {
    private static final Gson GSON;
    private String encoding = Base64.encode("admin:admin".getBytes());

    static {
        GSON = (new GsonBuilder()).setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    private static final Logger log = Loggers.get(AddCommentsPostJob.class);

    @Override
    public void describe(PostJobDescriptor postJobDescriptor) {
    }

    @Override
    public void execute(PostJobContext postJobContext) {
        Collection<EventsStatistic.ClassStat> statistic = eventsStatistic.getStatistic();
        for (EventsStatistic.ClassStat classStat : statistic) {
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


    private void addCommentsToIssuesPerRule(OverOpsMetrics.OverOpsMetric metric) {
        Pattern exceptionPattern = Pattern.compile("^"+ metric.patterName+"\\((.*?)\\)");
        String ruleType = metric.ruleFullKey;
        String severityType = metric.severity.toString().toUpperCase();


        //TODO remove this, all patterns should be filled
        if (StringUtils.isEmpty(metric.patterName)) {
            log.error("Rule should not be empty, metric will be postponed " + metric.overOpsType);
            return;
        }

        log.error("");
        log.error(" =============================================   ");
        log.error("addCommentsToIssuesPerRule  " + exceptionPattern.toString());

        SQSimpleRequest issuesPerRuleRequest = SQSimpleRequest.newBuilder()
                .setUrl(SONAR_HOST_URL + "/api/issues/search")
                .addQueryParam("rules", ruleType)
                .build();
        UrlClient.Response<String> stringResponse = SimpleUrlClient.newBuilder().build().get(issuesPerRuleRequest);

        log.info("");
        log.info("____________________________________________________");
        log.info("");
        log.info("");
        log.info(stringResponse.data);

        log.info("");
        log.info("");
        log.info("____________________________________________________");
        log.info("");

        SQIssuesResponse sqIssuesResponse = GSON.fromJson(stringResponse.data, SQIssuesResponse.class);
        log.info("sqIssuesResponse.issues count " + (sqIssuesResponse.issues != null ? String.valueOf(sqIssuesResponse.issues.size()) : "Empty"));
        for (SQIssue sqIssue :sqIssuesResponse.issues) {
            log.info("         " + sqIssue.status + "   key   " + sqIssue.key);
        }
        for (SQIssue sqIssue :sqIssuesResponse.issues) {

            if ((sqIssue.status == null) ||
                    (sqIssue.status.indexOf("OPEN") == -1)) {
                continue;
            }
            Matcher matcher = exceptionPattern.matcher(sqIssue.message);
            boolean matched = matcher.find();
            log.error("pattern match message " + matched);
            log.error("sqIssue.message " + sqIssue.message);
            if (matched) {
                String issueEventId = matcher.group(1);
                EventResult eventResult = getEventDataById(issueEventId);
                log.error("overops event found " + (eventResult != null));
                if (eventResult != null && !isOverOpsCommentAdded(sqIssue, eventResult, severityType)) {
                    addCommentToSonarQubeIssue(sqIssue, issueEventId, eventResult);
                }
            }
        }

        log.error("");
        log.error(" =============================================   ");
        log.error("");
    }

    private boolean isOverOpsCommentAdded(SQIssue sqIssue, EventResult eventResult, String severityType) {
        log.info("     isOverOpsCommentAdded");
        SQIssueFullData fullData = getComments(sqIssue, severityType);
        if (fullData != null && fullData.issue != null && fullData.issue.comments != null) {
            Pattern overOpsMessagePattern = Pattern.compile("Drill down into");
            Optional<SQComment> comment = fullData.issue.comments.stream().filter(sqComment -> {
                log.info("sqComment.markdown " + sqComment.markdown);
                if (sqComment.markdown != null) {
                    return overOpsMessagePattern.matcher(sqComment.markdown).find();
                }
                return false;
            }).findAny();
            log.error("Comment are present " + comment.isPresent());
            return comment.isPresent();
        }
        return false;
    }

    private SQIssueFullData getComments(SQIssue sqIssue, String severityType) {
        log.info("getComments");
        SQSimpleRequest request = SQSimpleRequest.newBuilder()
                .setUrl(SONAR_HOST_URL + "/api/issues/set_severity")
                .addQueryParam("issue", sqIssue.key)
                .addQueryParam("severity", severityType)
                .build();
        log.error(SONAR_HOST_URL + "/api/issues/set_severity" + "/?issue=" + sqIssue.key + "&severity=" + severityType);
        UrlClient.Response<String> stringResponse = SimpleUrlClient
                .newBuilder()
                .setAuth(getSonarQubeAuth())
                .build().post(request);

        if (stringResponse != null) {
            log.error("     comments are gotten ");
            return GSON.fromJson(stringResponse.data, SQIssueFullData.class);
        }

        return null;
    }

    private Pair<String, String> getSonarQubeAuth() {
        return Pair.of(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
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
                    .setAuth(Pair.of(HttpHeaders.AUTHORIZATION,"Basic " + encoding))
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
        log.error("Ret event from volume result by ID   " + issueEventId);
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
