package com.overops.plugins.sonar.rules.checks;

import com.overops.plugins.sonar.measures.OverOpsEventsStatistic;
import com.overops.plugins.sonar.measures.OverOpsMetrics;
import com.takipi.api.client.result.event.EventResult;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.overops.plugins.sonar.OverOpsPlugin.serviceId;
import static com.overops.plugins.sonar.OverOpsPlugin.overOpsEventsStatistic;

public abstract class OverOpsBaseExceptionCheck extends BaseTreeVisitor implements JavaFileScanner {
    private static final Logger log = Loggers.get(OverOpsBaseExceptionCheck.class);
    private JavaFileScannerContext context;
    private File file;
    protected OverOpsMetrics.OverOpsMetric metric;
    public static Map<String, Long> fileToLinesCount = new HashMap<>();

    @Override
    public void scanFile(final @Nonnull JavaFileScannerContext ctx) {
        if (overOpsEventsStatistic == null) {
            return;
        }

        context = ctx;
        file = context.getFile();
        String filePathJavaStyle = file.getAbsolutePath().replaceAll("/", ".");
        List<OverOpsEventsStatistic.ClassStat> statForThisFile = overOpsEventsStatistic.getStatistic()
                .stream()
                .filter(classStat -> {
//                    log.info( "           classStat.fileName [" + classStat.fileName + "] is in " + filePathJavaStyle + "  "
//                            + (filePathJavaStyle.indexOf(classStat.fileName) != -1));

                    if (filePathJavaStyle.indexOf(classStat.fileName) != -1) {
                        log.info( "");
                        log.info("           classStat.fileName [" + classStat.fileName + "] is in " + filePathJavaStyle);
                    }
                    return filePathJavaStyle.indexOf(classStat.fileName) != -1;})
                .filter(classStat -> {
                    //log.info( "           [" + qualityGateKey + "] is in " + String.join("/",classStat.qualityGateToEventStat.keySet()) + "  " + classStat.qualityGateToEventStat.keySet().contains(qualityGateKey));
                    if ( classStat.qualityGateToEventStat.keySet().contains(metric.qualityGateKey))
                        log.info( "           [" + metric.qualityGateKey + "] is in " + String.join("/",classStat.qualityGateToEventStat.keySet()));
                    return classStat.qualityGateToEventStat.keySet().contains(metric.qualityGateKey);
                })
                .collect(Collectors.toList());

        for (OverOpsEventsStatistic.ClassStat classStat : statForThisFile) {
            OverOpsEventsStatistic.EventInClassStat eventInClassStat = classStat.qualityGateToEventStat.get(metric.qualityGateKey);
            for (int lineNumber : eventInClassStat.lineToLineStat.keySet()) {
                reportIssue(eventInClassStat.lineToLineStat.get(lineNumber));
            }
        }

        scan(ctx.getTree());
    }

    public void reportIssue(OverOpsEventsStatistic.LineStat lineStat) {
        EventResult event = lineStat.event.eventResult;
        int method_position = event.error_location.original_line_number + 1;
        long fileCount = getFileCount();
        boolean isMethodPresent = fileCount >= method_position;
        method_position = isMethodPresent ? method_position : 1;
        //log.info(" ");
        //log.info("           method_position " + event.error_location.method_position + "           original_line_number " + event.error_location.original_line_number);
        //.info(" file lines : " + fileCount + "    overops line " + method_position + " is inside " + (isMethodPresent? " YES " : " NO ") + file.getPath());

        //String url = "https://api.overops.com/api/v1/services/"+ serviceId +"/events/" + event.id;
        //log.info("url " + url);
        log.info("                  Reporting " + lineStat.event.qualityGatesKey + " on " + lineStat.event.eventResult.error_location.prettified_name);
        String issueTitle = getIssueTitle(lineStat);

        context.addIssue(method_position , this, issueTitle);
    }

    private String getIssueTitle(OverOpsEventsStatistic.LineStat lineStat) {
        EventResult event = lineStat.event.eventResult;
        String qualityGateKey = String.join(" and ", metric.qualityGate);
        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append(qualityGateKey)
                .append(" ")
                .append(event.summary)
                .append(OverOpsMetrics.MESSAGE_PATTERN_PREFIX)
                .append(event.id)
                .append(OverOpsMetrics.MESSAGE_PATTERN_SUFFIX)
                .toString();
    }

    private long getFileCount() {
        try {
            String path = file.getPath();
            Long fileLines = fileToLinesCount.get(path);
            if (fileLines == null) {
                long count = Files.lines(file.toPath()).count();
                fileToLinesCount.put(path, count);
                return count;
            } else {
                return fileLines;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 1;
    }
}
