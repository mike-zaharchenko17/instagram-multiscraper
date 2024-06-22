package filters.maven;

import java.util.List;

import igscraperproject.maven.IGUser;

public class NaiveBioExclusionFilter extends Filter<String[]> {
	
	@Override
	public Boolean apply(IGUser u, String[] param) {
		// This is a naive implementation. It will not scale well.
		// I want to see if I can pull an Aho-Corasick library in to
		// take care of this more efficiently (god help me)
		String bio = u.getBiography();
		for (String exclusion : param) {
			if (bio.contains(exclusion)) {
				return false;
			}
		}
		return true;
	}
}