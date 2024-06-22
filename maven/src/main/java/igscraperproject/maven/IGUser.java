package igscraperproject.maven;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import com.google.gson.*;


public class IGUser implements Comparable<IGUser> {
	// json data
	
	// user data level metrics
	protected String username;
	protected String biography;
	protected String fullName;
	protected Boolean isPrivate;
	protected Integer totalPosts;
	
	// nested metrics
	protected Integer followers;
	protected Integer following;
	
	// calculated metrics - nested inside media nodes array
	protected Integer totalLikes;
	protected Integer totalComments;
	protected Double averageLikesPerPost;
	protected Double averageCommentsPerPost;
	protected Double averageEngagement;
	
	protected List<String> relatedUsers;
	
	public String getUsername() {
		return username;
	}


	public String getBiography() {
		return biography;
	}


	public String getFullName() {
		return fullName;
	}


	public Boolean getIsPrivate() {
		return isPrivate;
	}


	public Integer getTotalPosts() {
		return totalPosts;
	}


	public Integer getFollowers() {
		return followers;
	}


	public Integer getFollowing() {
		return following;
	}


	public Integer getTotalLikes() {
		return totalLikes;
	}


	public Integer getTotalComments() {
		return totalComments;
	}


	public Double getAverageLikesPerPost() {
		return averageLikesPerPost;
	}


	public Double getAverageCommentsPerPost() {
		return averageCommentsPerPost;
	}


	public Double getAverageEngagement() {
		return averageEngagement;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)  {
			return true;
		}
		
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}
		
		IGUser compareUser = (IGUser) o;
		
		return this.username.equals(compareUser.username);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(username);
	}

	@Override
	public int compareTo(IGUser otherUser) {
		return this.username.compareTo(otherUser.username);
	}
	
	@Override
	public String toString() {
		return "Username: " + username;
	}
}