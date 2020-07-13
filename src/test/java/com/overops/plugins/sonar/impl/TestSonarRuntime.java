package com.overops.plugins.sonar.impl;

import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

public class TestSonarRuntime implements SonarRuntime {
    @Override
    public Version getApiVersion() {
        return null;
    }

    @Override
    public SonarProduct getProduct() {
        return null;
    }

    @Override
    public SonarQubeSide getSonarQubeSide() {
        return null;
    }

    @Override
    public SonarEdition getEdition() {
        return null;
    }
}
