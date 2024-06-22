package igscraperproject.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import org.bson.Document;

import com.mongodb.client.*;
import dbsetup.maven.DatabaseConnectionSetup;

public class Controller {
	private BlockingQueue<Map<String, Map<String, ? extends Object>>> forUpdate;
	
	private ExecutorService updateConsumerThreadPool;
	private ExecutorService producerThreadPool;
	
	private MongoDatabase mongoDb;
	private MongoClient mongoClient;
	private MongoCollection<Document> collection;
	
	private DbUpdateConsumer dbU;
	
	private static final int MAX_QUEUE_SIZE = 50;
	
	
	public Controller() {
		DatabaseConnectionSetup dbSetup = new DatabaseConnectionSetup();
		
		mongoClient = dbSetup.getClient();
		mongoDb = dbSetup.getDatabase();
		
		collection = mongoDb.getCollection("iguser");
		
		forUpdate = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
		
		dbU = new DbUpdateConsumer(forUpdate, collection);
		
		producerThreadPool = Executors.newCachedThreadPool();
		updateConsumerThreadPool = Executors.newCachedThreadPool();
		
		updateConsumerThreadPool.submit(dbU);
	}
	
	public List<IGUser> dbFetchUsers() {
		List<IGUser> users = new ArrayList<>();
		for (Document doc : collection.find()) {
			IGUser currUser = new IGUser();
			IGUserFromDocFactory.populateIGUserWithDoc(currUser, doc);
			users.add(currUser);
		}
		return users;
	}
	
	private CompletableFuture<IGUser> createCompletableFuture(String username, Map<String, String> filterArgs) {
		ReadCallParseProducer producer = new ReadCallParseProducer(username, forUpdate, collection, filterArgs);
		CompletableFuture<IGUser> future = CompletableFuture.supplyAsync(() -> {
			try {
				return producerThreadPool.submit(producer).get();
			} catch (InterruptedException | ExecutionException e) {				
				if (e instanceof ExecutionException) {
					System.out.println("Received NullPointerException because currUser was null; "
							+ "likely the username was incorrect or "
							+ "you received a redirect because of rate limiting; "
						);
				} else {
					e.printStackTrace();
				}
				return null;
			}
		}, producerThreadPool);
		
		return future;
	}
	
	// returns a completable future with a set of unique IGUser objects
	// object as a promise to the UI. By doing this, we allow the UI to 
	// block the scrape button and display a progress spinner on startup 
	// and unblock the scrape button/hide the spinner on completion. In addition
	// to this, returning a CF that contains unique objects is critical to the TableView
	// update procedure. This ensures that we don't double update, especially in the
	// depth scrape procedure where duplicates are highly likely. Though converting it to
	// a set of IGUser objects is frivolous in the case of single scrape, it is done to
	// ensure consistency between what this method returns and what UiController expects.
	
	public CompletableFuture<Set<IGUser>> singleScrape(String username, Map<String, String> filterArgs) {
		updateConsumerThreadPool.submit(new DbUpdateConsumer(forUpdate, collection));
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Set<IGUser> uniqueUsers = ConcurrentHashMap.newKeySet();
		CompletableFuture<Set<IGUser>> future = createCompletableFuture(username, filterArgs)
				.thenApply((currUser) -> {
					if (currUser != null) {
						System.out.println(currUser.username + " visible in controller");
						uniqueUsers.add(currUser);
					}
					
					return uniqueUsers;
				});
		return future;
	}
	
	// same idea, but stores the results in a list and returns the
	// completablefuture when tasks associated with every list entry
	// have been completed
	
	public CompletableFuture<Set<IGUser>> arrayScrape(String[] usernames, Map<String, String> filterArgs) {
		updateConsumerThreadPool.submit(new DbUpdateConsumer(forUpdate, collection));
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Set<IGUser> uniqueUsers = ConcurrentHashMap.newKeySet();
		List<CompletableFuture<IGUser>> accountList = new ArrayList<>();
		
		for (String currUser : usernames) {
			CompletableFuture<IGUser> future = createCompletableFuture(currUser, filterArgs)
				.thenApply((user) -> {
					if (user != null) {
						uniqueUsers.add(user);
						System.out.println(user.username + " visible in controller");
					}
					return user;
				});
			
			accountList.add(future);
		}
		
		// Once all tasks finish, convert the value stored in the generated array
		// to a CF with only uniqueUsers to match the specified return type.
		return CompletableFuture.allOf(accountList.toArray(new CompletableFuture[0]))
				.thenApply((v) -> uniqueUsers);

	}
	
	// Old version of the method- for my reference
	
	
	// first draft of the method. It's not suitable for the UI
	// but it's being kept here for reference because it's logically
	// easy to follow and provides a decent reference for what 
	// depthScrapeCfVoid does. Modifier set to private.
	
	private void depthScrapeCfMv(String username, int depth, int maxDepth, Map<String, String> filterArgs) {
		if (depth > maxDepth) {
			System.out.println("Max depth reached");
			return;
		}
		
		System.out.println("Made function call for " + username);
		
		CompletableFuture<IGUser> future = createCompletableFuture(username, filterArgs);
		
		future.thenAccept((currUser) -> {
			if (currUser != null) {
				System.out.println(currUser.username + " visible in controller");
				if (currUser.relatedUsers != null) {
					for (String related : currUser.relatedUsers) {
						System.out.println("Recursing from " + currUser.username);
						depthScrapeCfMv(related, depth+1, maxDepth, filterArgs);
					}
					currUser.relatedUsers.forEach((related) -> {
						depthScrapeCfMv(related, depth+1, maxDepth, filterArgs);
					});
				}
			}
		})
		.exceptionally((e) -> {
			e.printStackTrace();
			return null;
		});
	}
	
	// Since we need to prevent the user from submitting more scraping jobs
	// while a scraping job is active, returning a CompletableFuture is
	// critical. This allows us to, in the UI, use the CF<Set<IGUser> obj as a promise
	// and chain actions to it once it completes.
	
	// Initially, this CF was populated with <Void>, but, once I hooked up the
	// TableView, I realized that this would mean that, to update the UI with the
	// new entries to the database, I would basically have to recreate the TableView
	// by reflecting everything from the database, and there would still be no
	// guarantee that it would be accurate because DB updates asynchronously. 
	// This was not efficient at all, so I modified the implementation to return a CF<Set<IGUser>>
	
	// With this type of depth scrape, there are often duplicates because a user A's related users often
	// include A's usernames. By using a set, we can filter out these duplicates and keep ourselves
	// from having to do unnecessary database lookups.
	
	// The recursion goes like this:
	
	// base case: depth exceeds maxDepth - return completed future with uniqueUser set
	// recursive case: depth <= maxDepth
	
	// For each call, we create a CF<IGUser>.
	// Once that returns, we begin composing with it.
	// If the current user is not null, we attempt to add them to the unique
	// users set. We also initialize a list of CFs parameterized to take Set<IGUser>
	// since each currUser has their own "level" of related users in the tree.
	
	// We then add the results of recursive calls for each related user in the current user's
	// list of related usernames, and we return a future that represents
	// the completion of all of those tasks. We return the result of the
	// composition at each level of recursion, but not before configuring it to
	// return the existing "version" of the Set they're all modifying. 
	// If the current user is null, we simply a CF with the uniqueUsers set
	// because there is nothing left to scrape on that branch.
	
	public CompletableFuture<Set<IGUser>> initDepthScrapeCfSet(String username, int depth, int maxDepth, Map<String, String> filterArgs) {
		updateConsumerThreadPool.submit(new DbUpdateConsumer(forUpdate, collection));
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Set<IGUser> returnSet = ConcurrentHashMap.newKeySet();
		// Since we want to add only entries that are not
		// already in the table to the table, we fill the set
		// with everything in the database to then diff it inside the
		// UiController class
		returnSet.addAll(dbFetchUsers());
		return depthScrapeCfSet(username, depth, maxDepth, filterArgs, returnSet);
	}
	
	public CompletableFuture<Set<IGUser>> depthScrapeCfSet(String username, int depth, int maxDepth, Map<String, String> filterArgs, Set<IGUser> uniqueUsers) {
		if (depth > maxDepth) {
			return CompletableFuture.completedFuture(uniqueUsers);
		}
		
		System.out.println("Made function call for " + username);
		
		CompletableFuture<IGUser> future = createCompletableFuture(username, filterArgs);
		
		return future.thenCompose((currUser) -> {
			if (currUser != null) {
				// Processing
				System.out.println(currUser.username + " visible in controller");
				boolean added = uniqueUsers.add(currUser);
				if (added) {
					System.out.println("Adding " + currUser.username + " to unique set");
				} else {
					System.out.println("Did not add " + currUser.username + " to unique set");
				}
				
				List<CompletableFuture<Set<IGUser>>> currentDepthList = new ArrayList<>();
				
				if (currUser.relatedUsers != null) {
					for (String related : currUser.relatedUsers) {
						currentDepthList.add(depthScrapeCfSet(related, depth+1, maxDepth, filterArgs, uniqueUsers));
					}
				}
				
	            return CompletableFuture.allOf(currentDepthList.toArray(new CompletableFuture[0]))
	                    .thenApply(v -> uniqueUsers);
			}
			
			return CompletableFuture.completedFuture(uniqueUsers);
		})
		.exceptionally((ex) -> {
			ex.printStackTrace();
			return uniqueUsers;
		});
	}
}