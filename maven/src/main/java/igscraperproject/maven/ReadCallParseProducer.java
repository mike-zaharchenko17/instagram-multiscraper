package igscraperproject.maven;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

public class ReadCallParseProducer implements Callable<IGUser> {
	private String targetUsername;
	private HttpClient client;
	private HttpRequest request;
	private MongoCollection<Document> collection;
	
	private BlockingQueue<Map<String, Map<String, ? extends Object>>> forUpdate;
	private static String endpoint = "https://www.instagram.com/";
	public static final int DELAY = 1000;
	
	private static String apiUsername = System.getenv("SMARTPROXY_API_USERNAME");
	private static String apiPassword = System.getenv("SMARTPROXY_API_PASSWORD");
	
	private Map<String, Map<String, ? extends Object>> userObjFilterArgsPayload;
	private Map<String, IGUser> userMap;
	private Map<String, String> filterArgsMap;
	
	protected ReadCallParseProducer(
			String targetUsername,
			BlockingQueue<Map<String, Map<String, ? extends Object>>> forUpdate,
			MongoCollection<Document> collection,
			Map<String, String> filterArgsMap
		) 
	{
		this.targetUsername = targetUsername;
		this.forUpdate = forUpdate;	
		this.collection = collection;
		
		this.client = HttpClient.newHttpClient();
		
		this.userObjFilterArgsPayload = new HashMap<>();
		this.userMap = new HashMap<>();
		
		this.filterArgsMap = filterArgsMap;
		
	}
	
	private static String getBasicAuthHeader(String apiUsername, String apiPassword) {
		String toEncode = apiUsername + ":" + apiPassword;
		return "Basic " + Base64.getEncoder().encodeToString(toEncode.getBytes());
	}
	
	private static HttpRequest buildRequest(String targetUsername, String apiUsername, String apiPassword) {
		String apiEndpoint = "https://scraper-api.smartproxy.com/v2/scrape";
		
		String payload = String.format(
				"""
				{
					"username": "%s",
					"target": "instagram_graphql_profile",
					"locale": "en-us",
					"geo": "United States"
				}
				"""
				, targetUsername);
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiEndpoint))
				.header("Content-Type", "application/json")
				.header("Authorization", getBasicAuthHeader(apiUsername, apiPassword))
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();
		
		return request;
	}
	
	@Override
	public IGUser call() {
		// make database read calls here before you make API requests- if
		// the object corresponding to the username is current, do not make
		// api call

		Document match = collection.find(Filters.eq("username", targetUsername)).first();
		IGUser activeUser;
		
		if (match != null) {
			System.out.println("Match found; querying db for " + targetUsername);
			activeUser = new IGUser();
			IGUserFromDocFactory.populateIGUserWithDoc(activeUser, match);
		} else {
			System.out.println("Match not found; making API call for " + targetUsername);
			HttpRequest request = buildRequest(targetUsername, apiUsername, apiPassword);
			HttpResponse<String> response;
			
			// send an HTTP request to the API endpoint
			// set response to what the api responds with
			// otherwise, if there is an exception, set it
			// to null
			try {
				response = client.send(request, HttpResponse.BodyHandlers.ofString());
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				response = null;
				e.printStackTrace();
			}
			
			// if there was no error with calling the api,
			// take the body of the response and parse it using
			// the ParseJson class. Otherwise set activeUser to null
			
			if (response != null) {
				System.out.println("Status code" + response.statusCode());
				String jsonString = response.body();
				// "catch" if there's a 302 redirect
				if (jsonString == null) {
					activeUser = null;
					// potentially add a throw declaration here
					// so that we can rotate proxy and try again
				} else {
					activeUser = new IGUser();
					// ParseJson will directly manipulate the passed IGUser object
					ParseJson.parseJsonString(activeUser, jsonString);
				}
			} else {
				System.out.println("Response body is null; something went wrong");
				activeUser = null;
			}
			
			
			// in this case, nothing was returned, so we either called the database
			// or API to retrieve the result If the result is not null, then we'll
			// update our session cache with it.
			
			// place the user object
			// into its map (filter args map is assembled in controller)
			userMap.put("userObject", activeUser);
			
			
			// place the maps into the payload map
			userObjFilterArgsPayload.put("user", userMap);
			userObjFilterArgsPayload.put("args", filterArgsMap);
			
			// put the activeUser object on the forUpdate queue to be sent to the
			// database update consumer
			try {
				forUpdate.put(userObjFilterArgsPayload);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("Interrupted while putting payload for " + activeUser.username + " on the update queue");
				e.printStackTrace();
			}
		}
		
		// return to caller (threadpool in Controller)
		return activeUser;
	}
} 