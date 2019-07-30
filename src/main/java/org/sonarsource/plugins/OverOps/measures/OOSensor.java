package org.sonarsource.plugins.OverOps.measures;

import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.plugins.OverOps.converter.OO_Talker;
import org.sonarsource.plugins.OverOps.settings.OverOpsProperties;

public class OOSensor implements Sensor{

	private static final Logger LOGGER = Loggers.get(OOSensor.class);	
	@Override
	public void describe(SensorDescriptor descriptor) {
		// TODO Auto-generated method stub
		descriptor.name("OverOps sensor calling OO_TALKER");
	}

	@Override
	public void execute(SensorContext context) {
		LOGGER.info("HOLY CRAP THIS PRINTED TO SOMETHING FINALLY");
		if (OverOpsProperties.APIKEY == null) {
			throw new IllegalStateException("APIKey is not filled in correctly");
		}
		RemoteApiClient apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(OverOpsProperties.APIKEY)
				.setHostname(OverOpsProperties.APPHOST).build();
		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, OverOpsProperties.OO_ENVID, "All Events");
		DateTimeFormatter dtf = ISODateTimeFormat.dateTime().withZoneUTC();

		DateTime to = DateTime.now();
		DateTime from = to.minusDays(1);

		EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder()
				.setServiceId(OverOpsProperties.OO_ENVID).setFrom(from.toString(dtf)).setTo(to.toString(dtf))
				.setViewId(view.id).setVolumeType(VolumeType.all).build();

		Response<EventsResult> eventsResponse = apiClient.get(eventsVolumeRequest);

		if (eventsResponse.isBadResponse())
			throw new IllegalStateException("Failed getting events.");
		
		LOGGER.info("This is a test string HELOOOOWIHPDGHSJKHGLSKJDGHLSDJGHLSD:GISPIOG:");
	}

	
}
