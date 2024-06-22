package igscraperproject.maven;

import org.bson.Document;

public class IGUserFromDocFactory {
	public static void populateIGUserWithDoc(IGUser user, Document doc) {
		// Choosing not to use a POJO to avoid unnecessary use of reflection
		// and also to have the option to implement interfaces in IGUser
		user.username = doc.getString("username");
		user.biography = doc.getString("biography");
		user.fullName = doc.getString("fullname");
		user.isPrivate = doc.getBoolean("isPrivate");
		user.totalPosts = doc.getInteger("totalPosts");
		user.followers = doc.getInteger("followers");
		user.following = doc.getInteger("following");
		user.totalLikes = doc.getInteger("totalLikes");
		user.totalComments = doc.getInteger("totalComments");
		user.averageLikesPerPost = doc.getDouble("averageLikesPerPost");
		user.averageCommentsPerPost = doc.getDouble("averageCommentsPerPost");
		user.averageEngagement = doc.getDouble("averageEngagement");
		user.relatedUsers = doc.getList("relatedUsers", String.class);
	}
}