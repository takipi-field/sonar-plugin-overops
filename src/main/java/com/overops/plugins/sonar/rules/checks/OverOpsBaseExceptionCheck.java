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
                .filter(classStat -> filePathJavaStyle.indexOf(classStat.fileName) != -1)
                .filter(classStat -> {
                    log.info(metric.overOpsType + " is in " +  classStat.typeToEventStat.keySet() + "  " + classStat.typeToEventStat.keySet().contains(metric.overOpsType));
                    return classStat.typeToEventStat.keySet().contains(metric.overOpsType);
                })
                .collect(Collectors.toList());

        for (OverOpsEventsStatistic.ClassStat classStat : statForThisFile) {
            OverOpsEventsStatistic.EventInClassStat eventInClassStat = classStat.typeToEventStat.get(metric.overOpsType);
            for (int lineNumber : eventInClassStat.lineToLineStat.keySet()) {
                reportIssue(eventInClassStat.lineToLineStat.get(lineNumber));
            }
        }

        scan(ctx.getTree());
    }

    public void reportIssue(OverOpsEventsStatistic.LineStat lineStat) {
        EventResult event = lineStat.event;
        int method_position = event.error_location.original_line_number + 1;
        long fileCount = getFileCount();
        boolean isMethodPresent = fileCount >= method_position;
        method_position = isMethodPresent ? method_position : 1;
        log.info("event.error_location.method_position " + event.error_location.method_position);
        log.info("event.error_location.original_line_number " + event.error_location.original_line_number);
        log.info(" file lines : " + fileCount + "    overops line " + method_position + " is inside " + (isMethodPresent? " YES " : " NO ") + file.getPath());

        String url = "https://api.overops.com/api/v1/services/"+ serviceId +"/events/" + event.id;
        log.info("url " + url);
        String issueTitle = getIssueTitle(lineStat);

        context.addIssue(method_position , this, issueTitle);
    }

    private String getIssueTitle(OverOpsEventsStatistic.LineStat lineStat) {
        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append(lineStat.event.summary)
                .append(OverOpsMetrics.MESSAGE_PATTERN_PREFIX)
                .append(lineStat.event.id)
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
