package org.sonarsource.plugins.OverOps.converter;

import org.sonarsource.plugins.OverOps.settings.OverOpsProperties;

import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;

public class OO_Talker {

	public static String callAllEvents() {
		RemoteApiClient apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(OverOpsProperties.APIKEY).setHostname(OverOpsProperties.APPHOST).build();
		EventRequest request = EventRequest.newBuilder().setEventId(OverOpsProperties.EVENTID).build(); 
		Response<EventResult> eventResponse = apiClient.get(request);
		if(eventResponse.isBadResponse()) {
			throw new IllegalStateException("Failed getting events");
		}
		return eventResponse.toString();
	}
	
	
	
	
	
}
