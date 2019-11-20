package com.overops.plugins.sonar.rules.checks;

import org.sonar.plugins.java.api.JavaCheck;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class OverOpsChecks {

    private static final Collection<Class<? extends JavaCheck>> defaultChecks = new ArrayList<Class<? extends JavaCheck>>();

    public static final String REPOSITORY_KEY = "OverOps-Rules";
    public static final String OVEROPS_ROOT_TAG = "overops";

    static {
//        defaultChecks.add(OverOpsCaughtExceptionCheck.class);
//        defaultChecks.add(OverOpsUncaughtExceptionCheck.class);
//        defaultChecks.add(OverOpsCustomEventCheck.class);
//        defaultChecks.add(OverOpsCriticalExceptionCheck.class);
//        defaultChecks.add(OverOpsHTTPErrorCheck.class);
//        defaultChecks.add(OverOpsLoggedErrorCheck.class);
//        defaultChecks.add(OverOpsSwallowedExceptionCheck.class);
        defaultChecks.add(OverOpsNewQualityGateCheck.class);
    }

    public static Collection<Class<? extends JavaCheck>> getChecks() {
        return Collections.unmodifiableCollection(defaultChecks);
    }
}
