package scraperui.maven;

import igscraperproject.maven.Controller;
import igscraperproject.maven.IGUser;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;

public class Main extends Application {
	
	protected static Controller backendController = new Controller();
	
	@FXML
	protected TableView<IGUser> mongoTable;
	
	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
		Parent root = loader.load();
		
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
		
		stage.setScene(scene);
		stage.setTitle("InfScraper");
		stage.show();
		
		stage.setFullScreen(true);
		stage.setOnCloseRequest((event) -> System.exit(0));
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
