package org.sonarsource.plugins.overops.measures;

import java.io.IOException;
import java.util.Optional;

import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventRequest;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.result.event.EventResult;
import com.takipi.api.client.result.event.EventsResult;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.plugins.overops.settings.OverOpsProperties;

public class OOSensor implements Sensor{

	public static EventsResult eventList = null;
	private static final Logger LOGGER = Loggers.get(OOSensor.class);	
	@Override
	public void describe(SensorDescriptor descriptor) {
		// TODO Auto-generated method stub
		descriptor.name("OverOps sensor calling OO_TALKER");
	}

	@Override
	public void execute(SensorContext context) {
		Configuration config = context.config();
		String envIdKey = config.get(OverOpsProperties.OO_ENVID).orElse(null);
		String appHost = config.get(OverOpsProperties.OO_URL).orElse(null);
		String apiKey = config.get(OverOpsProperties.APIKEY).orElse(null);
		
		if (OverOpsProperties.APIKEY == null) {
			throw new IllegalStateException("APIKey is not filled in correctly");
		}		
		
		RemoteApiClient apiClient = (RemoteApiClient) RemoteApiClient.newBuilder().setApiKey(apiKey)
				.setHostname(appHost).build();

		LOGGER.info(context.config().get("sonar.overops.environmentId").toString());
		SummarizedView view = ViewUtil.getServiceViewByName(apiClient, envIdKey, "All Events");
		
		DateTimeFormatter dtf = ISODateTimeFormat.dateTime().withZoneUTC();
		DateTime to = DateTime.now();
		DateTime from = to.minusDays(1);

		EventsVolumeRequest eventsVolumeRequest = EventsVolumeRequest.newBuilder()
				.setServiceId(envIdKey).setFrom(from.toString(dtf)).setTo(to.toString(dtf))
				.setViewId(view.id).setVolumeType(VolumeType.all).build();

		Response<EventsResult> eventsResponse = apiClient.get(eventsVolumeRequest);
		LOGGER.info(eventsResponse.toString());
		if (eventsResponse.isBadResponse())
			throw new IllegalStateException("Failed getting events.");
		eventList = eventsResponse.data;
		LOGGER.info("This is a test string HELOOOOWIHPDGHSJKHGLSKJDGHLSDJGHLSD:GISPIOG:");
		LOGGER.info(eventsResponse.data.events.get(0).toString());
		FileSystem fs = context.fileSystem();
		try {
			fs.baseDir().createNewFile();
		} catch (IOException e) {
			LOGGER.info("could not create file in the base directory in OOSensor");
			e.printStackTrace();
		}

		
	}

	
}
