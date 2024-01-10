/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.gui;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Cyril
 */
public class TrigraphsListController implements Initializable {

    // Reference to the main application
    private MainViewController mainViewController;

    private boolean okClicked = false;
    private Stage dialogStage;

    @FXML
    private Button btnValider;
    @FXML
    private Label labStatus;
    @FXML
    private TextArea taCrewList, taNewCrew;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    /**
     * Sets the stage of this dialog.
     *
     * @param dialogStage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void actionBtnValider(ActionEvent event) {
        okClicked = true;
        dialogStage.close();
    }

    /**
     * Returns true if the user clicked OK, false otherwise.
     *
     * @return
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    /**
     * Is called by the main application to give a reference back to itself.
     *
     * @param mainViewController
     */
    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;

        StringBuilder sb = new StringBuilder();
//        for (String string : mainViewController.getConnectFlights().getCrewList()) {
//            sb.append(string).append(System.lineSeparator());
//        }
        taCrewList.setText(sb.toString());
        
        sb = new StringBuilder();
//        for (String str : mainViewController.getConnectFlights().getFilteredCrewList()) {
//            sb.append(str).append(System.lineSeparator());
//        }
        taNewCrew.setText(sb.toString());
    }
}
