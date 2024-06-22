package igscraperproject.maven;

import java.util.Map;

public class TestEnv {
	public static void main(String[] args) {
		String env = System.getenv("MONGODB_CLUSTER0_USERNAME");
		System.out.println(env);
	}
}