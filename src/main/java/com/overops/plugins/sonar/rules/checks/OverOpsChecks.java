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
        initChecks(defaultChecks);
    }

    private static void initChecks(final @Nonnull Collection<Class<? extends JavaCheck>> checks) {
        checks.add(OverOpsCaughtExceptionCheck.class);
        checks.add(OverOpsUncaughtExceptionCheck.class);
    }

    public static Collection<Class<? extends JavaCheck>> getChecks() {
        return Collections.unmodifiableCollection(defaultChecks);
    }

    static Class<?>[] getChecksAsArray() {
        return getClassArray(getChecks());
    }

    private static Class<?>[] getClassArray(final @Nonnull Collection<Class<? extends JavaCheck>> checks) {
        return checks.toArray(new Class[checks.size()]);
    }

}
