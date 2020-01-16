package com.overops.plugins.sonar.util;

public class CommonMethods {
    public static String getJavaStyleFilePath(String filePath) {
        return filePath != null ? filePath.replaceAll("/", ".") : "";
    }
}
