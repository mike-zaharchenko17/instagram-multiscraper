package dbsetup.maven;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DatabaseConnectionSetup {
	private String connectionString;
	private ServerApi serverApi;
	private MongoClientSettings settings;
	private MongoDatabase db;
	
	private MongoClient client;
	
	private static String dbUsername = System.getenv("MONGODB_CLUSTER0_USERNAME");
	private static String dbPassword = System.getenv("MONGODB_CLUSTER0_PASSWORD");
	
	public DatabaseConnectionSetup() {
		connectionString = "mongodb+srv://" + dbUsername + ":" + dbPassword + "@cluster0.jfetmiy.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
		serverApi = ServerApi.builder()
				.version(ServerApiVersion.V1)
				.build();
		
		settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(connectionString))
				.serverApi(serverApi)
				.build();
		
		client = MongoClients.create(settings);
		db = client.getDatabase("iguserdb");
	}
	
	public MongoDatabase getDatabase() {
		return db;
	}
	
	public MongoClient getClient() {
		return client;
	}
	
	public void close() {
		if (client != null) {
			client.close();
		}
	}
}