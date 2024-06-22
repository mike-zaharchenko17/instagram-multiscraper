package filters.maven;

import igscraperproject.maven.IGUser;

public class MaxFollowersFilter extends Filter<Integer> {
	
	@Override
	public Boolean apply(IGUser u, Integer param) {
		// TODO Auto-generated method stub
		return (u.getFollowers() <= param);
	}
}