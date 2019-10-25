package com.overops.plugins.sonar.rules.checks;

import com.overops.plugins.sonar.OverOpsPlugin;
import com.overops.plugins.sonar.measures.EventsStatistic;
import com.takipi.api.client.result.event.EventResult;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Rule(key = "CaughtException",
        name = "CaughtException",
        description = "Over ops description",
        tags = {"over-ops", "oo-caught-exception"})
public class OverOpsCaughtExceptionCheck extends BaseTreeVisitor implements JavaFileScanner {
    private static final Logger log = Loggers.get(OverOpsCaughtExceptionCheck.class);
    private JavaFileScannerContext context;
    private File file;
    private String ooExceptionType = "Caught Exception";

    @Override
    public void scanFile(final @Nonnull JavaFileScannerContext ctx) {
        context = ctx;
        file = context.getFile();
        String filePathJavaStyle = file.getAbsolutePath().replaceAll("/", ".");
        log.info("filePathJavaStyle " + filePathJavaStyle);
        List<EventsStatistic.ClassStat> statForThisFile = OverOpsPlugin.eventsStatistic.getStatistic()
                .stream()
                .filter(classStat -> filePathJavaStyle.indexOf(classStat.fileName) != -1)
                .filter(classStat -> classStat.typeToEventStat.keySet().contains(ooExceptionType))
                .collect(Collectors.toList());

        for (EventsStatistic.ClassStat classStat : statForThisFile) {
            EventsStatistic.EventInClassStat eventInClassStat = classStat.typeToEventStat.get(ooExceptionType);
            for (int lineNumber : eventInClassStat.lineToLineStat.keySet()) {
                reportIssue(eventInClassStat.lineToLineStat.get(lineNumber));
            }
        }

        scan(ctx.getTree());
    }

    public void reportIssue(EventsStatistic.LineStat lineStat) {

        EventResult event = lineStat.event;
        int method_position = event.error_location.method_position + 1;
        boolean isMethodPresent = true;//file.lines >= method_position;
        method_position = isMethodPresent ? method_position : 1;

        String issueTitle = "CaughtException(" + event.id + ") has been detected " + lineStat.total + (lineStat.total > 1 ? " times" : " time");

        context.addIssue(method_position , this, issueTitle);
    }
}