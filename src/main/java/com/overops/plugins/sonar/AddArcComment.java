package com.overops.plugins.sonar;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonParser;
import com.overops.plugins.sonar.model.EventsJson;
import com.overops.plugins.sonar.model.IssueComment;
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

/**
 * Add Arc Screen Comment
 *
 * Post Job that reads the cached events and creates a Sonar comment with a link to the Arc screen
 */
public class AddArcComment implements PostJob {

	private static final Logger LOGGER = Loggers.get(AddArcComment.class);

	private URI sonarHostUri;
	private HttpHost targetHost;
	private HttpClientContext httpContext;
	private CloseableHttpClient httpClient;

	@Override
	public void describe(PostJobDescriptor descriptor) {
		descriptor.name("add issue comments with OverOps link");
	}

	@Override
	public void execute(PostJobContext context) {
		// add OverOps issue comment with ARC link

		LOGGER.info("Adding issue comments with OverOps links");

		try {
			List<EventsJson> eventsJson =  EventDataStore.instance().getData();
			LOGGER.info("Fetched Events: {}", eventsJson.size());

			// wait for 60 seconds for tasks to finish
			// data from scanner is not immediately available in the backend
			LOGGER.info("Waiting for background tasks...");
			String pauseForTheCasue = System.getProperty("overops.pauseForTheCause", "1");
			TimeUnit.MINUTES.sleep(Integer.parseInt(pauseForTheCasue));

			setHttpContext(context);
			httpClient = HttpClientBuilder.create().build();

			for (EventsJson events : eventsJson) {
				String componentKey = events.getComponentKey();
				String rule = events.getRule();
				List<IssueComment> issues = events.getIssues();

				String reqUri = sonarHostUri.toString() + "/api/issues/search?ps=500&additionalFields=comments"
						+ "&rules=" + rule + "&componentKeys=" + componentKey;

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
						addComment(issues, thisIssue, responseIssue, key);
					}
				} finally {
					res.close();
				}
			}
		} catch (Exception ex) {
			LOGGER.error("OverOps post job encountered an error.");
			LOGGER.error(ex.getMessage());

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);

			ex.printStackTrace(pw);

			LOGGER.error(sw.toString()); // stack trace as a string
		}

	}

	void setHttpContext(PostJobContext context) throws URISyntaxException {
		// see: https://docs.sonarqube.org/latest/extend/web-api/
		// login can be username or token. if token, password is blank.
		String login = context.config().get("sonar.login").orElse(null);
		String password = context.config().get("sonar.password").orElse("");
		String sonarHostUrl = context.config().get("sonar.host.url").orElse(null);

		// set sonarHostUri
		sonarHostUri = new URI(sonarHostUrl);

		// set targetHost
		targetHost = new HttpHost(sonarHostUri.getHost(), sonarHostUri.getPort(), sonarHostUri.getScheme());

		// set httpContext with credentials
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()),
				new UsernamePasswordCredentials(login, password));

		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();

		authCache.put(targetHost, basicAuth);

		httpContext = HttpClientContext.create();
		httpContext.setCredentialsProvider(credsProvider);
		httpContext.setAuthCache(authCache);
	}

	void addComment(List<IssueComment> issues, IssueComment thisIssue, JsonObject responseIssue, String key)
			throws URISyntaxException, IOException {
		if (issues.contains(thisIssue)) {

			IssueComment issue = issues.get(issues.indexOf(thisIssue));

			// check to see if this comment already exists
			boolean commentExists = false;
			JsonArray comments = responseIssue.get("comments").getAsJsonArray();

			for (JsonElement comment : comments) {
				JsonObject c = comment.getAsJsonObject();
				String markdown = c.get("markdown").getAsString();

				if (markdown.contains(IssueComment.LINK_TEXT)) {
					commentExists = true;
					break;
				}
			}

			// add comment if it doesn't already exist
			if (!commentExists) {
				URIBuilder builder = new URIBuilder(sonarHostUri.toString() + "/api/issues/add_comment");
				builder.setParameter("issue", key);
				builder.setParameter("text", issue.getComment());

				HttpPost post = new HttpPost(builder.build());
				CloseableHttpResponse postRes = httpClient.execute(targetHost, post, httpContext);

				try {
					if (postRes.getStatusLine().getStatusCode() != 200) {
						LOGGER.error("OverOps plugin was unable to add issue comment");
					} else {
						LOGGER.info("Comment added successfully [" + key + "]");
					}
				} finally {
					postRes.close();
				}

			} else {
				LOGGER.info("Comment not updated");
			}
		}
	}
}
