package com.overops.plugins.sonar;

import com.google.common.io.Files;
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
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.overops.plugins.sonar.OverOpsPlugin.*;
import static com.overops.plugins.sonar.measures.OverOpsMetrics.EXCEPTION_PATTERN;

public class AddCommentsPostJob implements PostJob {

    private static final Logger LOGGER = Loggers.get(AddCommentsPostJob.class);
    private final FileSystem fileSystem;
    private final Settings settings;

    private static final Gson GSON;

    static {
        GSON = (new GsonBuilder()).setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    private static final Logger log = Loggers.get(AddCommentsPostJob.class);

    public AddCommentsPostJob(FileSystem fileSystem, Settings settings) {
        this.fileSystem = fileSystem;
        this.settings = settings;
    }

    @Override
    public void describe(PostJobDescriptor postJobDescriptor) {
    }


    @Override
    public void execute(PostJobContext postJobContext) {
        if (overOpsEventsStatistic == null || overOpsEventsStatistic.getStatistic().size() == 0) {
            LOGGER.info("No OverOps issues for post job");
            return;
        }

        waitForSonarQubeCreatIssuesInDb();

        Collection<OverOpsEventsStatistic.ClassStat> statistic = overOpsEventsStatistic.getStatistic();
        for (OverOpsEventsStatistic.ClassStat classStat : statistic) {
            log.info("    " + classStat.fileName);
            for (String exName : classStat.qualityGateToEventStat.keySet()) {
                log.info("           " + exName);
            }
        }

        OverOpsMetrics.OverOpsMetric[] values = OverOpsMetrics.OverOpsMetric.values();
        for (OverOpsMetrics.OverOpsMetric metric : values) {
            addCommentsToIssuesPerRule(metric);
        }
    }

    private Properties loadReportTaskProps() {
        File reportTaskFile = new File(fileSystem.workDir(), "report-task.txt");
        Properties reportTaskProps = new Properties();

        try {
            reportTaskProps.load(Files.newReader(reportTaskFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.info("Unable to load properties from file " + reportTaskFile, e);
        }

        return reportTaskProps;
    }

    private void waitForSonarQubeCreatIssuesInDb() {
        try {
            Properties reportTaskProps = loadReportTaskProps();
            HttpConnector httpConnector =
                    HttpConnector.newBuilder()
                            .url(reportTaskProps.getProperty("serverUrl"))
                            .credentials(
                                    settings.getString(CoreProperties.LOGIN),
                                    settings.getString(CoreProperties.PASSWORD))
                            .build();

            WsClient wsClient = WsClientFactories.getDefault().newClient(httpConnector);
            String ceTaskId = reportTaskProps.getProperty("ceTaskId");
            WsRequest ceTaskRequest =
                    new GetRequest("api/ce/task").setParam("id", ceTaskId).setMediaType(MediaTypes.PROTOBUF);

            tryGetReport(wsClient, ceTaskRequest, 1000, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean tryGetReport(WsClient wsClient, WsRequest ceTaskRequest, int queryMaxAttempts, int queryInterval) {
        for (int attempts = 0; attempts < queryMaxAttempts; attempts++) {
            LOGGER.info("Call base url: " + wsClient.wsConnector() + ",  path : " + ceTaskRequest.getPath());
            WsResponse wsResponse = wsClient.wsConnector().call(ceTaskRequest);
            try {
                Ce.TaskResponse taskResponse = Ce.TaskResponse.parseFrom(wsResponse.contentStream());
                Ce.TaskStatus taskStatus = taskResponse.getTask().getStatus();

                switch (taskStatus) {
                    case IN_PROGRESS:
                    case PENDING:
                        LOGGER.info("Waiting for report processing to complete...");
                        Thread.sleep(queryInterval);
                        break;
                    case SUCCESS:
                        return true;
                    default:
                        LOGGER.info("Report processing did not complete successfully: " + taskStatus);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
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
        for (SQIssue sqIssue : sqIssuesResponse.issues) {
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
                    .setAuth(Pair.of(HttpHeaders.AUTHORIZATION, "Basic " + AUTH_DATA))
                    .build().post(addComment);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private String getDescription(String issueEventId, EventResult eventResult) {
        String stackTrace = new TextBuilder().addArray(eventResult.stack_frames, "> at ").build();

        return "*Drill down into* " +
                "[Event Analysis](" + getARCLinkForEvent(issueEventId) + ")" + "\n" +
                "*Stack trace:*\n" +
                stackTrace;
    }

    private EventResult getEventDataById(String issueEventId) {
        Optional<EventResult> first = OverOpsPlugin.volumeResult.events.stream().filter(eventResult -> eventResult.id.equals(issueEventId)).findFirst();
        return first.isPresent() ? first.get() : null;
    }

    private String getARCLinkForEvent(String eventId) {
        String arcLink = null;
        try {
            arcLink = EventUtil.getEventRecentLinkDefault(apiClient, serviceId, eventId, from, to,
                    Arrays.asList(applicationName), Arrays.asList(), Arrays.asList(deploymentName),
                    (int) (1440 * daysSpan)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (arcLink == null) {
            arcLink = "EventUtil.getEventRecentLinkDefault apiClient," +
                    "  \"" + serviceId +
                    "\" ,  \"" + eventId +
                    "\" ,  \"" + from.toString(formatter) +
                    "\" ,  \"" + to.toString(formatter) +
                    "\" , \"" + applicationName +
                    "\", , \"" + deploymentName + "\"" +
                    ", " + String.valueOf((int) (1440 * daysSpan)) + "";
            log.info(arcLink);
        }

        return arcLink;
    }
}
