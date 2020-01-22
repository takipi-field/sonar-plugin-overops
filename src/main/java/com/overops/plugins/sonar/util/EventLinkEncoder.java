package com.overops.plugins.sonar.util;

import com.takipi.common.util.TimeUtil;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

public class EventLinkEncoder {

    private static final String TEMPLATE = "{\"service_id\":\"%s\",\"viewport_strings\":{\"from_timestamp\":\"%s\"," +
            "\"to_timestamp\":\"%s\",\"until_now\":false,"+
            "\"machine_names\":[%s],\"agent_names\":[%s],\"deployment_names\":[%s],"+
            "\"request_ids\":[%s]},\"timestamp\":\"%s\"}";

    public static String encodeLink(String appUrl,
                                    Collection<String> apps, Collection<String> servers, Collection<String> deployments,
                                    String serviceId, String eventId, String similar_event_ids,
                                    DateTime from, DateTime to, int sourceCode) {

        long firstSeenMillis;

        try {
            firstSeenMillis = from.getMillis();
        }
        catch (Exception e) {
            firstSeenMillis = 0l;
        }

        long dataRetentionStartTime = to.minusDays(90).getMillis();

        long fromTimeMillis = (firstSeenMillis == 0l) ? (dataRetentionStartTime) :
                (Math.max(dataRetentionStartTime, firstSeenMillis - 60000));
        long toTimeMillis = to.getMillis();

        String jsonString = String.format(TEMPLATE, serviceId, String.valueOf(fromTimeMillis), String.valueOf(toTimeMillis),
                toList(servers), toList(apps), toList(deployments),
                toEventIdsList(eventId, similar_event_ids), TimeUtil.getMillisAsString(to));
        System.out.println("Before encoding str <" + jsonString + ">");
        String encode = Base64.getUrlEncoder().encodeToString(jsonString.getBytes());

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(appUrl).append("/tale.html?snapshot=").append(encode).append("&source=").append(sourceCode);

        return stringBuilder.toString();
    }

    private static String toEventIdsList(String eventId, String similar_event_ids) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(eventId);

        if (!StringUtils.isEmpty(similar_event_ids)) {
            stringBuilder.append(',').append(similar_event_ids);
        }

        return stringBuilder.toString();
    }

    private static String toList(Collection<String> col) {
        return toList(col, true);
    }

    private static String toList(Collection<String> col, boolean addQuotes) {
        List<String> list = new ArrayList<String>(col);

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);

            if (addQuotes) {
                stringBuilder.append('"');
            }

            stringBuilder.append(s);

            if (addQuotes) {
                stringBuilder.append('"');
            }

            if (i < list.size() - 1) {
                stringBuilder.append(',');
            }
        }

        return stringBuilder.toString();
    }
}
