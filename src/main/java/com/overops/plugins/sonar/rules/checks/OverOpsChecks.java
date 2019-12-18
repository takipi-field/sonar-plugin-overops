package com.overops.plugins.sonar.rules.checks;

import org.sonar.plugins.java.api.JavaCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class OverOpsChecks {

    private static final Collection<Class<? extends JavaCheck>> defaultChecks = new ArrayList<>();

    public static final String REPOSITORY_KEY = "OverOps-Rules";
    public static final String OVEROPS_ROOT_TAG = "overops";

    static {
        defaultChecks.add(OverOpsNewQualityGateCheck.class);
        defaultChecks.add(OverOpsCriticalQualityGateCheck.class);
        defaultChecks.add(OverOpsResurfacedQualityGateCheck.class);
        defaultChecks.add(OverOpsIncreasingQualityGateCheck.class);

        //Combo
        defaultChecks.add(OverOpsNewCriticalQualityGateCheck.class);
        defaultChecks.add(OverOpsResurfacedCriticalQualityGateCheck.class);
        defaultChecks.add(OverOpsIncreasingCriticalQualityGateCheck.class);
    }

    public static Collection<Class<? extends JavaCheck>> getChecks() {
        return Collections.unmodifiableCollection(defaultChecks);
    }
}
