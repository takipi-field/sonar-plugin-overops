package org.sonarsource.plugins.OverOps.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.tz.UTCProvider;
import org.sonarsource.plugins.OverOps.settings.OverOpsProperties;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.*;
import com.takipi.api.client.RemoteApiClient;
import com.takipi.api.client.data.view.SummarizedView;
import com.takipi.api.client.request.event.EventsVolumeRequest;
import com.takipi.api.client.util.validation.ValidationUtil.VolumeType;
import com.takipi.api.client.util.view.ViewUtil;
import com.takipi.api.core.url.UrlClient.Response;
import com.takipi.api.client.result.event.*;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public class OO_Talker {
	String baseUrl = null;
	private static final Logger LOGGER = Loggers.get(OO_Talker.class);

	public OO_Talker(Configuration configuration) {
		String tempBaseUrl = configuration.hasKey(CoreProperties.SERVER_BASE_URL)
				? configuration.get(CoreProperties.SERVER_BASE_URL).orElse(null)
				: configuration.get("sonar.host.url").orElse(null);
		if (tempBaseUrl == null) {
			tempBaseUrl = "http://localhost:9000";
		}
		if (!tempBaseUrl.endsWith("/")) {
			tempBaseUrl += "/";
		}
		System.out.println("Hello Jacob Goldverg");
		this.baseUrl = tempBaseUrl;
		LOGGER.debug("This is a broken message JG omg what the hellll ahhhh");
	}

	public void callAllEvents() {
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
		
		writeJsonFile(eventsResponse.data.toString(), "overops-events.txt");
	}

	public void writeJsonFile(String json, String fileName) {
		File file = new File(this.baseUrl, fileName);
		try {
			Files.write(Paths.get(file.getAbsolutePath()), json.getBytes(), StandardOpenOption.CREATE);
			LOGGER.info(("wrote OverOps file" + fileName));
		} catch (IOException e) {
			throw MessageException.of("Failed to write file " + file.toString(), e);
		}
	}

}
