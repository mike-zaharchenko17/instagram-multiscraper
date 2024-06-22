package igscraperproject.maven;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

public class ParseJson {
	public static void parseJsonString(IGUser user, String jsonString) {
		if (!jsonString.isEmpty()) {
			JsonElement jsonTree = JsonParser.parseString(jsonString);
			JsonObject jsonTreeObj = jsonTree.getAsJsonObject();
			
			// traverse through the JSON tree
			JsonArray results = jsonTreeObj.getAsJsonArray("results");
			
			JsonObject resultNode = results.get(0).getAsJsonObject();
			
			JsonObject content = resultNode.getAsJsonObject("content");
			
			JsonObject fullData = content.getAsJsonObject("data");
			
			JsonObject userData = fullData.getAsJsonObject("user");
			
			// Process user data level information
			user.username = userData.getAsJsonPrimitive("username").getAsString();
			user.biography = userData.getAsJsonPrimitive("biography").getAsString();
			user.fullName = userData.getAsJsonPrimitive("full_name").getAsString();
			user.isPrivate = userData.getAsJsonPrimitive("is_private").getAsBoolean();
			
			// Process and save nested information (followers, following)
			user.followers = userData.getAsJsonObject("edge_followed_by").getAsJsonPrimitive("count").getAsInt();
			user.following = userData.getAsJsonObject("edge_follow").getAsJsonPrimitive("count").getAsInt();
			
			// Process and save nested information (posts, related users, and additional media)
			
			JsonObject timelineMedia = userData.getAsJsonObject("edge_owner_to_timeline_media");
			
			user.totalPosts = timelineMedia.getAsJsonPrimitive("count").getAsInt();
			
			// make a try catch here to handle if timeline is empty
			JsonArray mediaNodes = timelineMedia.getAsJsonArray("edges");
			
			calculatePostMetrics(user, mediaNodes);
			
			// make a try catch here to handle if related users is empty
			JsonArray relatedUsers = userData.getAsJsonObject("edge_related_profiles").getAsJsonArray("edges");
			extractUsernames(user, relatedUsers);
		}
	}
	
	private static void extractUsernames(IGUser user, JsonArray relatedNodes) {
		// this will need to be adjusted to make it so it can handle if relatedUsers is null
		ArrayList<String> relatedUserList = new ArrayList<>();
		
		for (int i = 0; i < 4; i++) {
			JsonObject currContainer = relatedNodes.get(i).getAsJsonObject();
			JsonObject currNode = currContainer.getAsJsonObject("node");
			String username = currNode.getAsJsonPrimitive("username").getAsString();
			relatedUserList.add(username);
		}
		
		user.relatedUsers = relatedUserList;
	}
	
	private static void calculatePostMetrics(IGUser user, JsonArray mediaNodes) {
		int mnSize = mediaNodes.size();
		
		int totalComments = 0;
		int totalLikes = 0;
		double averageLikesPerPost = 0.0;
		double averageCommentsPerPost = 0.0;
		
		for (JsonElement container : mediaNodes) {
			JsonObject currContainer = container.getAsJsonObject();
			JsonObject currNode = currContainer.getAsJsonObject("node");
			
			int likesOnPost = currNode.getAsJsonObject("edge_liked_by").getAsJsonPrimitive("count").getAsInt();
			totalLikes += likesOnPost;
			
			int commentsOnPost = currNode.getAsJsonObject("edge_media_to_comment").getAsJsonPrimitive("count").getAsInt();
			totalComments += commentsOnPost;	
		}
		
		if (mnSize > 0) {
			averageLikesPerPost = totalLikes / mnSize;
			averageCommentsPerPost = totalComments / mnSize;
		}
		
		user.totalLikes = totalLikes;
		user.totalComments = totalComments;
		user.averageLikesPerPost = averageLikesPerPost;
		user.averageCommentsPerPost = averageCommentsPerPost;
		user.averageEngagement = (averageLikesPerPost + averageCommentsPerPost) / user.followers;
	}
}