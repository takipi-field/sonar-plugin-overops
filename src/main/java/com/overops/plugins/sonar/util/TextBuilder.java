package com.overops.plugins.sonar.util;

import com.takipi.api.client.data.event.Location;

import java.util.List;

public class TextBuilder {
    private StringBuilder stringBuilder = new StringBuilder();

    public TextBuilder add(String text) {
        stringBuilder.append(text);

        return this;
    }

    public TextBuilder addBold(String text) {
        stringBuilder.append("<strong>").append(text).append("</strong>");

        return this;
    }

    public TextBuilder addQuote(String text) {
        stringBuilder.append("<blockquote>").append(text).append("</blockquote>");

        return this;
    }

    public TextBuilder addHighlighted(String text) {
        stringBuilder.append("<pre>").append(text).append("</pre>");

        return this;
    }

    public TextBuilder addHighlightedQuote(String text) {
        stringBuilder.append("<blockquote><pre>").append(text).append("</pre></blockquote>");

        return this;
    }

    public TextBuilder addLink(String link, String nameOfLink) {
        stringBuilder.append("<a target=\"_blank\" rel=\"noopener noreferrer\" href=\"").append(link).append("\">").append(nameOfLink).append("</a>");

        return this;
    }

    public TextBuilder addBoldLink(String link, String nameOfLink) {
        stringBuilder.append("<a target=\"_blank\" rel=\"noopener noreferrer\" href=\"").append(link).append("\">").append("<strong>").append(nameOfLink).append("</strong>").append("</a>");

        return this;
    }

    public TextBuilder addEnter() {
        stringBuilder.append("<br>");

        return this;
    }

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


