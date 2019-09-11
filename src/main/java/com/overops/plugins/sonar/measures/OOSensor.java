package com.overops.plugins.sonar.measures;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.overops.plugins.sonar.settings.OverOpsProperties;
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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.joda.time.DateTime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/*
 *This class is responsible for calling anythign relating to pulling OverOps data and aggregating some counts
 *this classes lifecycle is heavily limited so anything brought in that is not saved will be gone before Sonar begins to display data
 *currently saving onto the context.module(). On a file is preferred because it will persist but as of now we dont have line numbers.
 */
public class OOSensor implements Sensor {

    private static final Logger LOGGER = Loggers.get(OOSensor.class);
    private static String ENVIDKEY;
    private static String APPHOST;
    private static String APIKEY;
    private static String DEPNAME;
    private static String APPNAME;
    private static Instant TODAY = Instant.now();
    private static Instant FROM;
    public static RemoteApiClient APICLIENT;
    public static long DAYS;
    public EventsResult eventList;
    public EventsVolumeRequest eventsVolumeRequest;
    // the event type labels
    public final String CAUGHTEXCEPTION = "Caught Exception";
    public final String SWALLOWEDEXCEPTION = "Swallowed Exception";
    public final String UNCAUGHTEXCEPTION = "Uncaught Exception";
    public final String LOGGEDERROR = "Logged Error";
    public final String CUSTOMEVENT = "Custom Event";
    public final String HTTPERROR = "HTTP Error";
    public final String CRITICALEXCEPTION = "Critical Exception";

    public HashMap<String, Integer> exceptions;

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
        ENVIDKEY = config.get(OverOpsProperties.ooENVID).orElse(null);
        APPHOST = config.get(OverOpsProperties.ooURL).orElse("https://api.overops.com");
        APIKEY = config.get(OverOpsProperties.apiKEY).orElse(null);
        DEPNAME = config.get(OverOpsProperties.depNAME).orElse(setDeploymentName());
        APPNAME = config.get(OverOpsProperties.appNAME).orElse(null);
        DAYS = config.getLong(OverOpsProperties.DAYS).orElse(1l);// default 1 day
        String viewId = config.get(OverOpsProperties.SONAR_OVEROPS_VIEWID).orElse("All Exceptions");
        LOGGER.info("VM changes 2");//TODO delete this line
        LOGGER.info("ENVIDKEY :" + ENVIDKEY);
        LOGGER.info("APPHOST :" + APPHOST);
        LOGGER.info("DEPNAME :" + DEPNAME);
        LOGGER.info("APPNAME :" + APPNAME);
        LOGGER.info("DAYS :" + DAYS);
        LOGGER.info("viewId :" + viewId);


        if (APIKEY == null || ENVIDKEY == null) {

            return;
        }

        APICLIENT = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(APIKEY).setHostname(APPHOST).build();
        // brings in all events based on time range
        SummarizedView view = ViewUtil.getServiceViewByName(APICLIENT, ENVIDKEY, viewId);

        FROM = TODAY.minus(DAYS, ChronoUnit.DAYS);

        // seperate cases if the user does not provide some information
        if (DEPNAME == null && APPNAME == null) {
            eventsVolumeRequest = buildEventsVolumeRequest(view);
        } else if (APPNAME == null) {
            eventsVolumeRequest = buildEventsVolumeRequestDeploymentName(view);
        } else if (DEPNAME == null) {
            eventsVolumeRequest = buildEventsVolumeRequestApplicationName(view);
        } else {
            eventsVolumeRequest = buildEventsVolumeRequestAppAndDepName(view);
        }

        Response<EventsResult> eventsResponse = APICLIENT.get(eventsVolumeRequest);

        // prepare the map values, to set the values of the Measures
        if (eventsResponse.data.events == null) {
            throw new IllegalStateException(
                    "Failed getting OverOps events, please check Application Name and Deployment Name.");
        } else if (eventsResponse.isBadResponse()) {
            throw new IllegalStateException("Bad API Response " + APPHOST + " " + DEPNAME + " " + APPNAME);
        }
        eventList = eventsResponse.data;
        CountEvents countEvents = new CountEvents(eventsResponse.data);
        LOGGER.info("made it past exceptions");

        // classname to exception type to count
        HashMap<String, HashMap<String, Integer>> classToErrorTypeMethodCounts = countEvents.countClassErrors();
        LOGGER.info("made it past classErrorCounts Map");
        setFileContexts(context, classToErrorTypeMethodCounts, countEvents);
        LOGGER.info("Set the file contexts");
        // setMethodHighlights(context, countEvents); this does not work
        createExternalIssuePerEvent(context);

    }

    public String getTargetClassName(EventResult event) {
        String shortEnedName;
        if (event.error_location.class_name.contains(".")) {
            int lastPeriod = event.error_location.class_name.lastIndexOf('.');
            shortEnedName = event.error_location.class_name.substring(lastPeriod + 1,
                    event.error_location.class_name.length());
        } else {
            shortEnedName = event.error_location.class_name;
        }
        return shortEnedName;
    }

    public String shortenFileName(InputFile file) {
        return file.filename().substring(0, file.filename().lastIndexOf("."));
    }

    public void createExternalIssuePerEvent(SensorContext context) {
        FileSystem fs = context.fileSystem();

        Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
        Map<String, Integer> usages = new ConcurrentHashMap<>();
        for (InputFile file : files) {
            int lineCount = 1;
            String shortFileName = shortenFileName(file);
            for (EventResult event : eventList.events) {
                String shortClassName = getTargetClassName(event);
//                LOGGER.info(file.filename());

                String projectRelativePath = "";
                if (file instanceof DefaultInputFile) {
                    DefaultInputFile defaultInputFile = (DefaultInputFile) file;
                    projectRelativePath = defaultInputFile.getProjectRelativePath().replaceAll("/",  ".");
//                    LOGGER.info(" File in CD : " + projectRelativePath);
                }
//                LOGGER.info("Full file name: " + file.filename());
//                LOGGER.info("Full class name: " + event.error_location.class_name);
//                LOGGER.info("Short file name: " + shortFileName);
//                LOGGER.info("Short class name: " + shortClassName);

                if (projectRelativePath.indexOf(event.error_location.class_name) != -1) {
//                    LOGGER.info(" File in OO : " + event.error_location.class_name);
//                    LOGGER.info("  ");
                    if (usages.get(shortFileName) == null) {
//                        LOGGER.info(" VM in equals : " + usages.get(shortFileName) + "  " + shortClassName);
                        usages.put(shortFileName, 1);

                        createIssue(context, file, event, lineCount);


                        //NewHighlighting highlighting = context.newHighlighting();

                        int methodLine = event.error_location.method_position;
                        LOGGER.info(event.error_location.class_name + " methodLine = " + methodLine);
                        context.newAnalysisError().onFile(file).at(file.newPointer(methodLine + 1, 0)).message("newAnalysisError").save();

                        //highlighting.onFile(file).highlight(file.newRange(2, 0,  3, 0), TypeOfText.STRUCTURED_COMMENT).save();
                        lineCount++;
                        continue;
                    } else {

                        usages.put(shortFileName, usages.get(shortFileName) + 1);
                        LOGGER.info(" VM encrease counter : " + usages.get(shortFileName) + "  " + shortClassName);
                        continue;
                    }
                }

            }
        }
    }

    public void createIssue(SensorContext context, InputFile file, EventResult event, int lineNum) {
        NewExternalIssue newIssue = context.newExternalIssue();
        String arcLink = getARCLinkForEvent(event.id);
        LOGGER.info(file.filename());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<a href=\"").append(arcLink).append("\">").append("View Error on OverOps").append("</a>");
        int method_position = event.error_location.method_position;
        newIssue.engineId("OverOps Plugin").ruleId(event.id)
                .at(newIssue.newLocation().on(file).at(file.selectLine(method_position + 1))
                        //.message("This code has troubles please look http://myService.com ")
                       .message("Error: " + event.message + " Method that had the error: " + event.error_location.method_name + " link to ARC screen: " + arcLink)
                )
                .severity(getSeverity(event.type))
                .type(getType(event.type)).save();
    }

    /*
     * This method pulls in the tiny link based on the eventId passed
     */
    private String getARCLinkForEvent(String eventId) {
        LOGGER.info("entering call for tinylinks");
        DateTime today = DateTime.now();
        DateTime from = today.minus(100);
        String arcLink = EventUtil.getEventRecentLinkDefault(APICLIENT, ENVIDKEY, eventId, from, today, Arrays.asList(APPNAME), Arrays.asList(), Arrays.asList(DEPNAME), (int) (1140 * DAYS));
        LOGGER.info(arcLink);
        return arcLink;
    }

    public RuleType getType(String type) {
        switch (type) {
            case LOGGEDERROR:
                return RuleType.CODE_SMELL;
            case CAUGHTEXCEPTION:
                return RuleType.CODE_SMELL;
            default:
                return RuleType.BUG;
        }
    }

    public Severity getSeverity(String type) {
        switch (type) {
            case LOGGEDERROR:
                return Severity.MINOR;
            case HTTPERROR:
                return Severity.MINOR;
            default:
                return Severity.MAJOR;
        }
    }

    private EventsVolumeRequest buildEventsVolumeRequest(SummarizedView view) {
        return EventsVolumeRequest.newBuilder().setServiceId(ENVIDKEY.toUpperCase()).setFrom(FROM.toString())
                .setTo(TODAY.toString()).setViewId(view.id).setVolumeType(VolumeType.all).build();
    }

    private EventsVolumeRequest buildEventsVolumeRequestDeploymentName(SummarizedView view) {
        return EventsVolumeRequest.newBuilder().setServiceId(ENVIDKEY.toUpperCase()).setFrom(FROM.toString())
                .setTo(TODAY.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addDeployment(DEPNAME)
                .build();
    }

    private EventsVolumeRequest buildEventsVolumeRequestApplicationName(SummarizedView view) {
        return EventsVolumeRequest.newBuilder().setServiceId(ENVIDKEY.toUpperCase()).setFrom(FROM.toString())
                .setTo(TODAY.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addApp(APPNAME).build();
    }

    private EventsVolumeRequest buildEventsVolumeRequestAppAndDepName(SummarizedView view) {
        return EventsVolumeRequest.newBuilder().setServiceId(ENVIDKEY.toUpperCase()).setFrom(FROM.toString())
                .setTo(TODAY.toString()).setViewId(view.id).setVolumeType(VolumeType.all).addApp(APPNAME)
                .addDeployment(DEPNAME).build();
    }

    public void setFileContexts(SensorContext context, HashMap<String, HashMap<String, Integer>> classErrorCountMap,
                                CountEvents eventCount) {
        FileSystem fs = context.fileSystem();
        Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN));
        for (InputFile file : files) {
            String shortEnedName = file.filename().substring(0, file.filename().indexOf('.'));
            if (classErrorCountMap.containsKey(shortEnedName)) {
                for (String exceptionType : classErrorCountMap.get(shortEnedName).keySet()) {
                    context.<Integer>newMeasure().forMetric(eventCount.typeToMetricMap.get(exceptionType)).on(file)
                            .withValue(classErrorCountMap.get(shortEnedName).get(exceptionType)).save();
                }
            }
        }
    }

    // BNY uses the <version> tag in the pom.xml
    public String setDeploymentName() {
        String ret = "";
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new FileReader("pom.xml"));
            ret = model.getVersion();
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to read the file:");
            e.printStackTrace();
        } catch (IOException e) {
            LOGGER.error("Failed to read the file:");
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            LOGGER.error("Failed to read the file:");
            e.printStackTrace();
        }
        return ret;
    }
}
