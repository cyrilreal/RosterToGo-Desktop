package com.pluszero.rostertogo;

import com.pluszero.rostertogo.gui.MainViewController;
import java.util.HashMap;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static UserPrefs userPrefs = new UserPrefs();
    private HashMap<String, String> trigraphs;

    private FXMLLoader loader;
    private MainViewController mainViewController;
    private BorderPane rootLayout;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {

        this.primaryStage = primaryStage;
        loader = new FXMLLoader(MainApp.class.getResource("/fxml/MainView.fxml"));
        rootLayout = (BorderPane) loader.load();
        mainViewController = loader.getController();
        mainViewController.setMainApp(this);

        Scene scene = new Scene(rootLayout);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/img/icon.png")));
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        super.stop(); //To change body of generated methods, choose Tools | Templates.
        userPrefs.savePrefs();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
