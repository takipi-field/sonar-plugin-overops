package com.overops.plugins.sonar.measures;

import com.overops.plugins.sonar.settings.OverOpsProperties;
import com.overops.plugins.sonar.util.TextBuilder;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.event.EventUtil;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.FileReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;

/*
 *This class is responsible for calling anythign relating to pulling OverOps data and aggregating some counts
 *this classes lifecycle is heavily limited so anything brought in that is not saved will be gone before Sonar begins to display data
 *currently saving onto the context.module(). On a file is preferred because it will persist but as of now we dont have line numbers.
 */
public class OverOpsSensor implements Sensor {
    // the event type labels
    public static final String CAUGHT_EXCEPTION = "Caught Exception";
    public static final String SWALLOWED_EXCEPTION = "Swallowed Exception";
    public static final String UNCAUGHT_EXCEPTION = "Uncaught Exception";
    public static final String LOGGED_ERROR = "Logged Error";
    public static final String CUSTOM_EVENT = "Custom Event";
    public static final String HTTP_ERROR = "HTTP Error";
    public static final String CRITICAL_EXCEPTION = "Critical Exception";

    private static final Logger LOGGER = Loggers.get(OverOpsSensor.class);
    public static final long DEFAULT_SPAN_DAYS = 1l;
    public static final String DEFAULT_VIEWID = "All Exceptions";
    public static final String DEFAULT_OVER_OPS_API_HOST = "https://api.overops.com";
    public static final String OVER_OPS_ENGINE = "OverOps Plugin";

    private String environmentKey;
    private String appHost;
    private String apiKey;
    private String deploymentName;
    private String applicationName;
    private Instant to;
    private Instant from;
    private RemoteApiClient apiClient;
    private long daysSpan;

    public HashMap<String, Integer> exceptions;
    private String viewId;

    // This is displaying in logs(maven output) so when this sensor runs this gets
    // displayed
    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name(
                "OverOps sensor calling the Summarized View API with customer configuration and setting up the default Measures");
    }

    @Override
    public void execute(SensorContext context) {
        Configuration config = context.config();
        getConfigProperties(config);

        validateConfigData();

        apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(apiKey).setHostname(appHost).build();
        SummarizedView view = ViewUtil.getServiceViewByName(apiClient, environmentKey, viewId);
        if (view == null) {
            throw new IllegalStateException(
                    "Failed getting OverOps View, please check viewId or environmentKey.");
        }

        Response<EventsResult> volumeResponse = getVolumeResponse(view);

        validateVolumeResponse(volumeResponse);

        EventsResult volumeResult = volumeResponse.data;

        applyOverOpsEvent(context, volumeResult);
    }

    private Response<EventsResult> getVolumeResponse(SummarizedView view) {
        LOGGER.info("passed into  getVolumeResponse view " + view);
        EventsVolumeRequest eventsVolumeRequest = getVolumeRequest(view);
        return apiClient.get(eventsVolumeRequest);
    }

    private void validateVolumeResponse(Response<EventsResult> eventsResponse) {
        //TODO refactore it error should be thrown if bad response
        if (eventsResponse.data.events == null) {
            throw new IllegalStateException(
                    "Failed getting OverOps events, please check Application Name or Deployment Name.");
        } else if (eventsResponse.isBadResponse()) {
            throw new IllegalStateException("Bad API Response " + appHost + " " + deploymentName + " " + applicationName);
        }
    }

    private void validateConfigData() {
        if (StringUtils.isEmpty(apiKey) ||
                StringUtils.isEmpty(environmentKey) ||
                StringUtils.isEmpty(appHost) ||
                StringUtils.isEmpty(viewId)) {
            throw new IllegalStateException(
                    "Failed to process OverOps sensor one of [apiKey, environmentKey, appHost, viewId] properties is empty. Please check project OverOps configuration.");
        }
    }

    private EventsVolumeRequest getVolumeRequest(SummarizedView view) {
        EventsVolumeRequest.Builder builder = EventsVolumeRequest.newBuilder().setServiceId(environmentKey.toUpperCase()).setFrom(from.toString())
                .setTo(to.toString()).setViewId(view.id).setVolumeType(VolumeType.all).setIncludeStacktrace(true);

        if (StringUtils.isNotEmpty(deploymentName)) builder.addDeployment(deploymentName);
        if (StringUtils.isNotEmpty(applicationName)) builder.addApp(applicationName);

        return builder.build();
    }

    private void getConfigProperties(Configuration config) {
        environmentKey = config.get(OverOpsProperties.SONAR_OVEROPS_ENVIRONMENT_ID).orElse(null);
        appHost = config.get(OverOpsProperties.SONAR_OVEROPS_API_HOST).orElse(DEFAULT_OVER_OPS_API_HOST);
        apiKey = config.get(OverOpsProperties.SONAR_OVEROPS_API_KEY).orElse(null);
        deploymentName = config.get(OverOpsProperties.SONAR_OVEROPS_DEP_NAME).orElse(getDeploymentNameFromPomFile());
        applicationName = config.get(OverOpsProperties.SONAR_OVEROPS_APP_NAME).orElse(null);
        daysSpan = config.getLong(OverOpsProperties.SONAR_OVEROPS_SPAN_DAYS).orElse(DEFAULT_SPAN_DAYS);
        viewId = config.get(OverOpsProperties.SONAR_OVEROPS_VIEW_ID).orElse(DEFAULT_VIEWID);
        to = Instant.now();
        from = to.minus(daysSpan, ChronoUnit.DAYS);

        logConfigData();
    }

    private void logConfigData() {
        LOGGER.info("environmentKey :" + environmentKey);
        LOGGER.info("appHost :" + appHost);
        LOGGER.info("deploymentName :" + deploymentName);
        LOGGER.info("applicationName :" + applicationName);
        LOGGER.info("daysSpan :" + daysSpan);
        LOGGER.info("viewId :" + viewId);
    }

    public void applyOverOpsEvent(SensorContext context, EventsResult volumeResult) {
        FileSystem fs = context.fileSystem();
        EventsStatistic eventsStatistic = new EventsStatistic(volumeResult);

        Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
        for (InputFile file : files) {

            for (EventResult event : volumeResult.events) {
                String fileClassName = convertInputFileToClassName(file);

                if (fileClassName.indexOf(event.error_location.class_name) != -1) {
                    eventsStatistic.add(file, event);
                }
            }
        }

        for (EventsStatistic.ClassStat classStat : eventsStatistic.getStatistic()) {
            for (String eventType : classStat.typeToEventStat.keySet()) {

                context.<Integer>newMeasure().forMetric(eventsStatistic.getMetric(eventType)).on(classStat.file)
                        .withValue(classStat.typeToEventStat.get(eventType).total).save();

                LOGGER.info("     On " + classStat.file.filename() +
                        "  was OO [ " + eventType +
                        " ]  times  = " + classStat.typeToEventStat.get(eventType).total);

                EventsStatistic.EventInClassStat eventInClassStat = classStat.typeToEventStat.get(eventType);
                for (int lineNumber : eventInClassStat.lineToLineStat.keySet()){
                    attachExternalOverOpsIssue(context, classStat.file, eventInClassStat.lineToLineStat.get(lineNumber));
                    LOGGER.info("       add Issue  [ " + eventType +
                            " ]  on  = " + lineNumber);
                }
            }
        }
    }

    public static String convertInputFileToClassName(InputFile file) {
        String projectRelativePath = "";
        if (file instanceof DefaultInputFile) {
            DefaultInputFile defaultInputFile = (DefaultInputFile) file;
            projectRelativePath = defaultInputFile.getProjectRelativePath().replaceAll("/", ".");
        }
        return projectRelativePath;
    }

    public void attachExternalOverOpsIssue(SensorContext context, InputFile file, EventsStatistic.LineStat lineStat) {
        EventResult event = lineStat.event;
        int method_position = event.error_location.method_position + 1;
        boolean isMethodPresent = file.lines() >= method_position;
        method_position = isMethodPresent ? method_position : 1;
        String arcLink = getARCLinkForEvent(event.id);

        NewAdHocRule newAdHocRule = context.newAdHocRule();
        String issueTitle = "A " + event.name +" has been detected " + lineStat.total + (lineStat.total > 1 ? " times" : "time");
        String name = "Details for  a " + event.name +" that was detected " + lineStat.total + (lineStat.total > 1 ? " times" : "time");;
        String ruleId = environmentKey + "." + from.getNano() + "." + lineStat.event.type + "." +lineStat.event.id;

        TextBuilder textBuilder = new TextBuilder();
        if (!isMethodPresent) {
            textBuilder.add("<div style=\"color:red\">")
                    .addBold("Class " + event.error_location.class_name + "  is out of sync with OverOps version ")
                    .add("</div>")
                    .addEnter();
        }
        String description = textBuilder
                .addBold("Rich details can be found ")
                .addLink(arcLink, event.name + " detected " + lineStat.total + (lineStat.total > 1 ? " times" : "time"))
                .addEnter()
                .addBold("Stack trace :")
                .addEnter()
                .addHighlightedQuote(new TextBuilder().addArray(event.stack_frames, " at ").build())
                .build();

        newAdHocRule.description(description)
                .engineId(OVER_OPS_ENGINE)
                .name(name)
                .ruleId(ruleId)
                .type(RuleType.BUG)
                .severity(Severity.INFO).save();

        NewExternalIssue newIssue = context.newExternalIssue();
        newIssue.engineId(OVER_OPS_ENGINE).ruleId(event.id)
                .at(newIssue.newLocation().on(file).at(file.selectLine(method_position))
                        .message( issueTitle + ". Click \"See Rule\" for more details")
                )
                .severity(getSeverity(event.type))
                .ruleId(ruleId)
                .type(getType(event.type)).save();
    }

    private String getARCLinkForEvent(String eventId) {
        DateTime today = DateTime.now();
        DateTime from = today.minusDays((int)daysSpan);
        String arcLink = EventUtil.getEventRecentLink(apiClient, environmentKey, eventId, from, today, Arrays.asList(applicationName), Arrays.asList(), Arrays.asList(deploymentName));
        return arcLink;
    }

    private RuleType getType(String type) {
        switch (type) {
            case LOGGED_ERROR:
            case CAUGHT_EXCEPTION:
                return RuleType.CODE_SMELL;
            default:
                return RuleType.BUG;
        }
    }

    private Severity getSeverity(String type) {
        switch (type) {
            case LOGGED_ERROR:
            case HTTP_ERROR:
                return Severity.MINOR;
            default:
                return Severity.MAJOR;
        }
    }

    public String getDeploymentNameFromPomFile() {
        String result = "";
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new FileReader("pom.xml"));
            result = model.getVersion();
        } catch (Exception e) {
            LOGGER.error("Couldn't get DeploymentName from pom.xml version tag.");
            e.printStackTrace();
        }

        return result;
    }
}
