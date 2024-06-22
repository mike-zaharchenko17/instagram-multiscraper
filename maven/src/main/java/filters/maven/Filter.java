package filters.maven;
import java.util.function.BiFunction;

import igscraperproject.maven.IGUser;

public abstract class Filter<T extends Object> implements BiFunction<IGUser, T, Boolean> {
	@Override
	public abstract Boolean apply(IGUser u, T param); 
}