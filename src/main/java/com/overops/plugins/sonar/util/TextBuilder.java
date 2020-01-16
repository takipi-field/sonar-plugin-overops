package com.overops.plugins.sonar.util;

import com.takipi.api.client.data.event.Location;

import java.util.List;

public class TextBuilder {
    private StringBuilder stringBuilder = new StringBuilder();

    public String build() {
        return stringBuilder.toString();
    }


    public TextBuilder addArray(List<String> stack_frames, String prefix) {
        if (stack_frames == null) return this;

        for (String stack_frame : stack_frames) {
            stringBuilder.append(prefix).append(stack_frame).append("\n");
        }

        return this;
    }
}


