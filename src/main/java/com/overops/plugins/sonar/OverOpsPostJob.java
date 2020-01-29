package com.overops.plugins.sonar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.overops.plugins.sonar.JsonStore.STORE_FILE;

public class OverOpsPostJob implements PostJob {

	private static final Logger LOGGER = Loggers.get(OverOpsPostJob.class);

	@Override
	public void describe(PostJobDescriptor descriptor) {
		descriptor.name("add issue comments with OverOps link");
	}

	@Override
	public void execute(PostJobContext context) {
		// add OverOps issue comment with ARC link

		LOGGER.info("Adding issue comments with OverOps links");

		String login = context.config().get("sonar.login").orElse(null);
		String password = context.config().get("sonar.password").orElse(null);
		String sonarHostUrl = context.config().get("sonar.host.url").orElse(null);

		BufferedReader storeFile;
		try {
			storeFile = new BufferedReader(new FileReader(STORE_FILE));

			// convert the json file back to object
			JsonStore jsonStore = new Gson().fromJson(storeFile, JsonStore.class);

			List<EventsJson> eventsJson = jsonStore.getEventsJson();

			CloseableHttpClient httpClient = HttpClientBuilder.create().build();

			URI sonarHostUri = new URI(sonarHostUrl);
			HttpHost targetHost = new HttpHost(sonarHostUri.getHost(), sonarHostUri.getPort(), sonarHostUri.getScheme());

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()),
					new UsernamePasswordCredentials(login, password));

			AuthCache authCache = new BasicAuthCache();
			BasicScheme basicAuth = new BasicScheme();

			authCache.put(targetHost, basicAuth);

			HttpClientContext httpContext = HttpClientContext.create();
			httpContext.setCredentialsProvider(credsProvider);
			httpContext.setAuthCache(authCache);

			// wait for 60 seconds for tasks to finish
			// data from scanner is not immediately available in the backend
			LOGGER.info("Waiting for background tasks...");
			TimeUnit.MINUTES.sleep(1);

			for (EventsJson events : eventsJson) {
				String componentKey = events.getComponentKey();
				String rule = events.getRule();
				List<IssueComment> issues = events.getIssues();

				String reqUri = sonarHostUri.toString() + "/api/issues/search?ps=500&rules=" + rule + "&componentKeys="
						+ componentKey;

				HttpGet req = new HttpGet(reqUri);
				CloseableHttpResponse res = httpClient.execute(targetHost, req, httpContext);

				try {
					String response = EntityUtils.toString(res.getEntity());
					EntityUtils.consume(res.getEntity());

					// parse the response
					JsonParser parser = new JsonParser();
					JsonObject root = parser.parse(response).getAsJsonObject();

					int total = root.get("total").getAsInt();
					if (total < 1) {
						LOGGER.warn("Issue not found, cannot add comment");
					}

					JsonArray responseIssues = root.getAsJsonArray("issues");

					for (JsonElement responseElement : responseIssues) {
						JsonObject responseIssue = responseElement.getAsJsonObject();

						String key = responseIssue.get("key").getAsString();
						JsonObject textRange = responseIssue.get("textRange").getAsJsonObject();

						int line = textRange.get("startLine").getAsInt();
						String message = responseIssue.get("message").getAsString();

						// compare line number and message
						IssueComment thisIssue = new IssueComment();
						thisIssue.setLine(line);
						thisIssue.setMessage(message);

						// add link as a comment
						if (issues.contains(thisIssue)) {
							IssueComment issue = issues.get(issues.indexOf(thisIssue));

							URIBuilder builder = new URIBuilder(sonarHostUri.toString()+ "/api/issues/add_comment");
							builder.setParameter("issue", key);
							builder.setParameter("text",issue.getComment());

							HttpPost post = new HttpPost(builder.build());
							CloseableHttpResponse postRes = httpClient.execute(targetHost, post, httpContext);

							if (postRes.getStatusLine().getStatusCode() != 200) {
								LOGGER.error("OverOps plugin was unable to add issue comment");
							} else {
								LOGGER.info("Comment added successfully [" + key + "]");
							}

							postRes.close();
						}
					}
				} finally {
					res.close();
				}

			}

			// clean up
			File store = new File(STORE_FILE);
			if (!store.delete()) {
				LOGGER.warn("Unable to delete overops_db.json.");
			}

		} catch (FileNotFoundException ex) {
			LOGGER.info("overops_db.json not found: skipping OverOps post job");
		} catch (Exception ex) {
			LOGGER.error("OverOps post job encountered an error.");
			LOGGER.error(ex.getMessage());

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);

			ex.printStackTrace(pw);

			LOGGER.error(sw.toString()); // stack trace as a string
		}

	}

}
