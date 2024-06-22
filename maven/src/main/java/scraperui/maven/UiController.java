package scraperui.maven;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import igscraperproject.maven.Controller;
import igscraperproject.maven.IGUser;

import io.github.palexdev.materialfx.controls.MFXProgressSpinner;

public class UiController implements Initializable {
	
	private final int DEFAULT_MAX_DEPTH = 2;
	
	@FXML
	private TextField minFollowers;
	
	@FXML
	private TextField maxFollowers;
	
	@FXML
	private TextField exclKeywords;
	
	@FXML 
	private ComboBox<String> scrapeStyleCb;
	private String[] choices = {"Single", "List", "Depth"};
	
	@FXML
	private TextField targetUsernames;
	
	@FXML
	private Button scrapeButton;
	
	@FXML
	private MFXProgressSpinner progressSpinner;
	
	@FXML 
	private TextField scrapeDepth;
	
	// Table
	@FXML
	private TableView<IGUser> mongoTable;
	
	private TableColumn<IGUser, String> usernameCol;
	private TableColumn<IGUser, String> bioCol;
	private TableColumn<IGUser, String> fullNameCol;
	private TableColumn<IGUser, Boolean> isPrivateCol;
	private TableColumn<IGUser, Integer> totalPostsCol;
	private TableColumn<IGUser, Integer> followersCol;
	private TableColumn<IGUser, Integer> followingCol;
	private TableColumn<IGUser, Integer> totalLikesCol;
	private TableColumn<IGUser, Integer> totalCommentsCol;
	private TableColumn<IGUser, Double> averageLikesPerPostCol;
	private TableColumn<IGUser, Double> averageCommentsPerPostCol;
	private TableColumn<IGUser, Double> averageEngagementCol;
	
	private ObservableList<IGUser> userList;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		scrapeStyleCb.getItems().addAll(choices);
		scrapeStyleCb.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
		createTableView();
	}
	
	private void createTableView() {
		userList = FXCollections.observableArrayList(Main.backendController.dbFetchUsers());
		mongoTable.setItems(userList);
		
		usernameCol = new TableColumn<>("Username");
		usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
		
		bioCol = new TableColumn<>("Bio");
		bioCol.setCellValueFactory(new PropertyValueFactory<>("biography"));
		
		fullNameCol = new TableColumn<>("Full Name");
		fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
		
		isPrivateCol = new TableColumn<>("Private?");
		isPrivateCol.setCellValueFactory(new PropertyValueFactory<>("isPrivate"));
		
		totalPostsCol = new TableColumn<>("Total Posts");
		totalPostsCol.setCellValueFactory(new PropertyValueFactory<>("totalPosts"));
		
		followersCol = new TableColumn<>("Followers");
		followersCol.setCellValueFactory(new PropertyValueFactory<>("followers"));
		
		followingCol = new TableColumn<>("Following");
		followingCol.setCellValueFactory(new PropertyValueFactory<>("following"));
		
		totalLikesCol = new TableColumn<>("Total Likes");
		totalLikesCol.setCellValueFactory(new PropertyValueFactory<>("totalLikes"));
		
		totalCommentsCol = new TableColumn<>("Total Comments");
		totalCommentsCol.setCellValueFactory(new PropertyValueFactory<>("totalComments"));
		
		averageLikesPerPostCol = new TableColumn<>("Averages Likes Per Post");
		averageLikesPerPostCol.setCellValueFactory(new PropertyValueFactory<>("averageLikesPerPost"));
		
		averageCommentsPerPostCol = new TableColumn<>("Averages Comments Per Post");
		averageCommentsPerPostCol.setCellValueFactory(new PropertyValueFactory<>("averageCommentsPerPost"));		
		
		averageEngagementCol = new TableColumn<>("Average Engagement");
		averageEngagementCol.setCellValueFactory(new PropertyValueFactory<>("averageEngagement"));
		
		mongoTable.getColumns().add(usernameCol);
		mongoTable.getColumns().add(bioCol);
		mongoTable.getColumns().add(fullNameCol);
		mongoTable.getColumns().add(isPrivateCol);
		mongoTable.getColumns().add(totalPostsCol);
		mongoTable.getColumns().add(followersCol);
		mongoTable.getColumns().add(followingCol);
		mongoTable.getColumns().add(totalLikesCol);
		mongoTable.getColumns().add(totalCommentsCol);
		mongoTable.getColumns().add(averageLikesPerPostCol);
		mongoTable.getColumns().add(averageCommentsPerPostCol);
		mongoTable.getColumns().add(averageEngagementCol);
	}
	
	// This will pull the entire database in and recreate the table
	// I don't like this method but it's currently the only way for
	// "full" database integrity in not showing results that have been filtered out
	private void updateTableBruteForce() {
		userList = FXCollections.observableArrayList(Main.backendController.dbFetchUsers());
		mongoTable.setItems(userList);
	}
	
	private void updateTableWithUniqueSet(Set<IGUser> uniqueSet) {
		userList.addAll(uniqueSet);
	}
	
	private void handleIncomingFuture(CompletableFuture<Set<IGUser>> future, List<IGUser> dbSnapshot) {
		Platform.runLater(() -> {
			scrapeButton.setDisable(true);
			progressSpinner.setVisible(true);
		});

		future.thenAccept((res) -> {
			System.out.println("Successfully finished scraping");
			System.out.println("Full res: " + res);
			// TRIAL RUN
			res.removeAll(dbSnapshot);
			System.out.println("Unique res: " + res);
			System.out.println("Unique res length: " + res.size());
			
			Platform.runLater(() -> {
				updateTableWithUniqueSet(res);
				mongoTable.refresh();
				progressSpinner.setVisible(false);
				scrapeButton.setDisable(false);
			});
			
		})
		.exceptionally((ex) -> {
			System.out.println("Finished scrape with errors");
			Platform.runLater(() -> {
				updateTableBruteForce();
				mongoTable.refresh();
				scrapeButton.setDisable(false);
				progressSpinner.setVisible(false);
			});
			return null;
		});
	}
	
	private Map<String, String> assembleFilterArgs(
			String maxFollowers,
			String minFollowers,
			String bioExclusions
	) 
	{
		Map<String, String> toReturn = new HashMap<>();
		
		if (maxFollowers != null) {
			if (maxFollowers.isEmpty()) {
				maxFollowers = null;
			}
		}

		if (minFollowers != null) {
			if (minFollowers.isEmpty()) {
				minFollowers = null;
			}
		}

		if (bioExclusions != null) {
			if (bioExclusions.isEmpty()) {
				bioExclusions = null;
			}
		}

		toReturn.put("maxF", maxFollowers);
		toReturn.put("minF", minFollowers);
		toReturn.put("bioExcl", bioExclusions);
		
		return toReturn;
	}
	
	public void router(ActionEvent e) {
		String minF = minFollowers.getText(); // can be empty
		String maxF = maxFollowers.getText(); // can be empty
		String bioExcl = exclKeywords.getText(); // can be empty
		String scrapeDepthVal = scrapeDepth.getText();
		String scrapeStyle = scrapeStyleCb.getValue(); // cannot be empty
		String usernames = targetUsernames.getText(); // cannot be empty
		
		if (scrapeStyle != null && usernames != null) {
			if (scrapeStyle.isBlank() || usernames.isBlank()) {
				System.out.println("Both scrape style and usernames must be set");
				return;
			}
			Map<String, String> filterArgs = assembleFilterArgs(maxF, minF, bioExcl);
			String[] usernamesArray = usernames.split(", ");
			String firstUsername = usernamesArray[0];
			
			// TRIAL RUN
			List<IGUser> dbSnapshot = Main.backendController.dbFetchUsers();
			
			CompletableFuture<Set<IGUser>> future = null;
			
			if (scrapeStyle.equals("Single")) {
				future = Main.backendController.singleScrape(firstUsername, filterArgs);
			}
			
			if (scrapeStyle.equals("List")) {
				future = Main.backendController.arrayScrape(usernamesArray, filterArgs);
			}
			
			if (scrapeStyle.equals("Depth")) {
				Integer scrapeDepthInt = null;
				
				if (!(scrapeDepthVal.isBlank() || scrapeDepthVal == null)) {
					try {
						scrapeDepthInt = Integer.parseInt(scrapeDepthVal);
					} catch (NumberFormatException parseException) {
						System.out.println("Failed parse of depth integer; defaulting");
					}
				}
				
				if (scrapeDepthInt == null) {
					future = Main.backendController.initDepthScrapeCfSet(firstUsername, 0, DEFAULT_MAX_DEPTH, filterArgs);
				} else {
					future = Main.backendController.initDepthScrapeCfSet(firstUsername, 0, scrapeDepthInt, filterArgs);
				}	
			}
			
			if (future != null) {
				handleIncomingFuture(future, dbSnapshot);
			}
			
			
		} else {
			System.out.println("Both scrape style and usernames must be set");
			return;
		}
	}
}