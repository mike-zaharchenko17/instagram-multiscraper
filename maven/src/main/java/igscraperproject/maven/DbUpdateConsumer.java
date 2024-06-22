package igscraperproject.maven;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;

import filters.maven.*;

public class DbUpdateConsumer implements Runnable {
	private BlockingQueue<Map<String, Map<String, ? extends Object>>> forUpdate;
	private MongoCollection<Document> collection;
	
	private static MaxFollowersFilter maxF = new MaxFollowersFilter();
	private static MinFollowersFilter minF = new MinFollowersFilter();
	private static NaiveBioExclusionFilter bioExcl = new NaiveBioExclusionFilter();
	
	protected DbUpdateConsumer(
			BlockingQueue<Map<String, Map<String, ? extends Object>>> forUpdate, 
			MongoCollection<Document> collection
		) {
		this.forUpdate = forUpdate;
		this.collection = collection;
	}
	
	
	public static Boolean passesAllFilters(IGUser user, Map<String, ? extends Object> filterMap) {
		for (String s : filterMap.keySet()) {
			String activeString = (String) filterMap.get(s);
			if (s.equals("maxF")) {
				if (activeString != null) {
					Integer maxFollowersInt = Integer.parseInt(activeString);					
					if (!(maxF.apply(user, maxFollowersInt))) {
						System.out.println(user.username + " did not pass maxF filter");
						return false;
					} else {
						System.out.println(user.username + " passed maxF filter");
					}
				} else {
					System.out.println("Did not run maxF test- param not specified");
				}
			} else if (s.equals("minF")) {
				if (activeString != null) {
					Integer minFollowersInt = Integer.parseInt(activeString);
					if (!(minF.apply(user, minFollowersInt))) {
						System.out.println(user.username + " did not pass minF filter");
						return false;
					} else {
						System.out.println(user.username + " passed minF filter");
					}
				} else {
					System.out.println("Did not run minF test- param not specified");
				}
			} else {
				if (activeString != null) {
					String[] exclTerms = activeString.split(", ");
					for (String curr : exclTerms) {
						curr = curr.strip();
					}
					if (!(bioExcl.apply(user, exclTerms))) {
						System.out.println(user.username + " did not pass bioExcl filter");
						return false;
					} else {
						System.out.println(user.username + " passed bioExcl filter");
					}
				} else {
					System.out.println("Did not run bio exclusion test- param not specified");
				}
			}
		}
		return true;
	}
	
	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			Map<String, Map<String, ? extends Object>> userObjFilterArgsMap = null;
			IGUser extractedUser = null;
			Map<String, ? extends Object> userMap = null;
			Map<String, ? extends Object> filterArgs = null;
			
			try {
				userObjFilterArgsMap = forUpdate.take();
				
				userMap = userObjFilterArgsMap.get("user");
				extractedUser = (IGUser) userMap.get("userObject");
				
				filterArgs = userObjFilterArgsMap.get("args");
				
			} catch (InterruptedException e) {
				System.out.println("Interrupted while taking ref off of queue");
				Thread.currentThread().interrupt();
				break;
			}
			
			if (extractedUser != null) {
				if (passesAllFilters(extractedUser, filterArgs)) {
					System.out.println("Updating database with data for: " + extractedUser.username);
					Document doc = new Document("_id", new ObjectId())
							.append("username", extractedUser.username)
							.append("biography", extractedUser.biography)
							.append("fullName", extractedUser.fullName)
							.append("isPrivate", extractedUser.isPrivate)
							.append("totalPosts", extractedUser.totalPosts)
							.append("followers", extractedUser.followers)
							.append("following", extractedUser.following)
							.append("totalLikes", extractedUser.totalLikes)
							.append("totalComments", extractedUser.totalComments)
							.append("averageLikesPerPost", extractedUser.averageLikesPerPost)
							.append("averageCommentsPerPost", extractedUser.averageCommentsPerPost)
							.append("averageEngagement", extractedUser.averageEngagement)
							.append("relatedUsers", extractedUser.relatedUsers);
					InsertOneResult result = collection.insertOne(doc);
					System.out.println("Database successfully updated for: " + extractedUser.username);
					System.out.println(result);
				} else {
					System.out.println("Did not pass all filters; DB not updated");
				}
			}
		}
	}
}