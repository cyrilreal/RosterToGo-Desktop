/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.gui;

import com.pluszero.rostertogo.MainApp;
import com.pluszero.rostertogo.Utils;
import com.pluszero.rostertogo.online.SyncGoogleCalendar;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Cyril
 */
public class OptionsViewController implements Initializable {

    // Reference to the main application
    private boolean okClicked = false;
    private Stage dialogStage;

    @FXML
    private CheckBox cbxAutoComputeHc;
    @FXML
    private TextField tfldFilesSavingLocation;
    @FXML
    private CheckBox cbxAutoCheckAndSign;
    @FXML
    private CheckBox cbxUsePdfData;
    @FXML
    private CheckBox cbxShowAcceptedCrewRequest;
    @FXML
    private CheckBox cbxColorizeEvents;
    @FXML
    private Button btnValider;
    @FXML
    private TextField tfldGoogleCalendarsFlight;
    @FXML
    private TextField tfldGoogleCalendarsGround;
    @FXML
    private TextField tfldGoogleCalendarsOff;
    @FXML
    private TextField tfldGoogleCalendarsVacation;
    @FXML
    private TextField tfldGoogleCalendarsBlanc;
    @FXML
    private Button btnDeleteEvents;
    @FXML
    private TextField tfldDeleteEventCalendars;
    @FXML
    private DatePicker dpkrDeleteEventsDateBegin;
    @FXML
    private DatePicker dpkrDeleteEventsDateEnd;
    @FXML
    private Label labStatus;
    @FXML
    private Button btnDeleteStoredCredential;
    @FXML
    private Button btnDeletePreferences;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadPrefs();
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
        savePrefs();

        okClicked = true;
        dialogStage.close();
    }

    @FXML
    private void actionBtnDeleteEvents(ActionEvent event) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        Date begin;
        Date end;
        calendar.set(Calendar.DAY_OF_MONTH, dpkrDeleteEventsDateBegin.getValue().getDayOfMonth());
        calendar.set(Calendar.MONTH, dpkrDeleteEventsDateBegin.getValue().getMonthValue() - 1);
        calendar.set(Calendar.YEAR, dpkrDeleteEventsDateBegin.getValue().getYear());
        begin = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, dpkrDeleteEventsDateEnd.getValue().getDayOfMonth());
        calendar.set(Calendar.MONTH, dpkrDeleteEventsDateEnd.getValue().getMonthValue() - 1);
        calendar.set(Calendar.YEAR, dpkrDeleteEventsDateEnd.getValue().getYear());
        end = calendar.getTime();

        String calendars = tfldDeleteEventCalendars.getText();

        SyncGoogleCalendar sgc = new SyncGoogleCalendar(begin, end, calendars);
        sgc.setMode(SyncGoogleCalendar.MODE_DELETE_EVENTS);
        btnDeleteEvents.setDisable(true);
        sgc.setOnSucceeded((WorkerStateEvent ev) -> {
            labStatus.textProperty().unbind();
            if (!ev.getSource().getValue().equals(SyncGoogleCalendar.MSG_EVENTS_DELETED)) {
                labStatus.setText("Echec de la suppression des evenements");
            } else {
                labStatus.setText("Evenements supprimés");
                labStatus.textProperty().unbind();
            }
            btnDeleteEvents.setDisable(false);
        });
        labStatus.textProperty().bind(sgc.messageProperty());
        new Thread(sgc).start();
    }

    @FXML
    private void actionTargetedCalendars(MouseEvent me) {
        if (me.getSource().equals(tfldGoogleCalendarsFlight)) {
            showUserCalendarsView(SyncGoogleCalendar.CALENDAR_FLIGHT);
        } else if (me.getSource().equals(tfldGoogleCalendarsGround)) {
            showUserCalendarsView(SyncGoogleCalendar.CALENDAR_GROUND);
        } else if (me.getSource().equals(tfldGoogleCalendarsOff)) {
            showUserCalendarsView(SyncGoogleCalendar.CALENDAR_OFF);
        } else if (me.getSource().equals(tfldGoogleCalendarsVacation)) {
            showUserCalendarsView(SyncGoogleCalendar.CALENDAR_VACATION);
        } else if (me.getSource().equals(tfldGoogleCalendarsBlanc)) {
            showUserCalendarsView(SyncGoogleCalendar.CALENDAR_BLANC);
        } else if (me.getSource().equals(tfldDeleteEventCalendars)) {
            showUserCalendarsView(SyncGoogleCalendar.CALENDAR_EVENTS_DELETE);
        }
    }

    @FXML
    private void actionBtnDeleteStoredCredential(ActionEvent event) {
        Alert alertConfirm = new Alert(AlertType.WARNING);
        alertConfirm.setTitle("Attention !");
        alertConfirm.setHeaderText("Etes vous sûr ?");
        alertConfirm.setContentText("Si vous supprimez ce fichier, RosterToGo devra redemander l'autorisation d'accès à Google");
        Optional<ButtonType> result = alertConfirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            File path = new File(System.getProperty("user.home"), ".RosterToGo/");
            if (Utils.deleteDirectory(path)) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Information");
                alert.setContentText("Le certificat d'accès à Google a été supprimé");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Attention !");
                alert.setContentText("Erreur lors de la suppression du certificat d'accès à Google");
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void actionBtnDeletePreferences(ActionEvent event) {
        Alert alertConfirm = new Alert(AlertType.WARNING);
        alertConfirm.setTitle("Attention !");
        alertConfirm.setHeaderText("Etes vous sûr ?");
        alertConfirm.setContentText("Vous allez réinitialiser toutes les préférences de RosterToGo !");
        Optional<ButtonType> result = alertConfirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Preferences.userRoot().node("RosterToGo").removeNode();
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Information");
                alert.setContentText("Les préférences ont été réinitialisées, RosterToGo va se fermer pour terminer cette opération");
                alert.showAndWait();
            } catch (BackingStoreException ex) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Attention !");
                alert.setContentText("Erreur lors de la réinitialisation des préférences, RosterToGo va se fermer pour terminer cette opération");
                alert.showAndWait();
            } finally {
                System.exit(0);
            }
        }
    }

    @FXML
    private void actionFilesSavingLocation(MouseEvent me) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Dossier cible pour les fichiers ics et pdf");
        File defaultDirectory = new File(MainApp.userPrefs.latestLoadLocation);
        chooser.setInitialDirectory(defaultDirectory);
        File selectedDirectory = chooser.showDialog(tfldFilesSavingLocation.getScene().getWindow());

        if (selectedDirectory != null) {
            tfldFilesSavingLocation.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * Returns true if the user clicked OK, false otherwise.
     *
     * @return
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    private void savePrefs() {
        MainApp.userPrefs.filesSavingLocation = tfldFilesSavingLocation.getText();
        MainApp.userPrefs.autoCheckAndSign = cbxAutoCheckAndSign.isSelected();
        MainApp.userPrefs.usePdfData = cbxUsePdfData.isSelected();
        MainApp.userPrefs.showAcceptedCrewRequest = cbxShowAcceptedCrewRequest.isSelected();
        MainApp.userPrefs.filesSavingLocation = tfldFilesSavingLocation.getText();
        MainApp.userPrefs.googleColorizeEvents = cbxColorizeEvents.isSelected();
        MainApp.userPrefs.googleCalendarsFlight = tfldGoogleCalendarsFlight.getText();
        MainApp.userPrefs.googleCalendarsGround = tfldGoogleCalendarsGround.getText();
        MainApp.userPrefs.googleCalendarsOff = tfldGoogleCalendarsOff.getText();
        MainApp.userPrefs.googleCalendarsVacation = tfldGoogleCalendarsVacation.getText();
        MainApp.userPrefs.googleCalendarsBlanc = tfldGoogleCalendarsBlanc.getText();
        
        MainApp.userPrefs.autoComputeCreditHours = cbxAutoComputeHc.isSelected();
    }

    private void loadPrefs() {
        cbxAutoCheckAndSign.setSelected(MainApp.userPrefs.autoCheckAndSign);
        cbxUsePdfData.setSelected(MainApp.userPrefs.usePdfData);
        cbxShowAcceptedCrewRequest.setSelected(MainApp.userPrefs.showAcceptedCrewRequest);
        tfldFilesSavingLocation.setText(MainApp.userPrefs.filesSavingLocation);
        
        cbxColorizeEvents.setSelected(MainApp.userPrefs.googleColorizeEvents);
        tfldGoogleCalendarsFlight.setText(MainApp.userPrefs.googleCalendarsFlight);
        tfldGoogleCalendarsGround.setText(MainApp.userPrefs.googleCalendarsGround);
        tfldGoogleCalendarsOff.setText(MainApp.userPrefs.googleCalendarsOff);
        tfldGoogleCalendarsVacation.setText(MainApp.userPrefs.googleCalendarsVacation);
        tfldGoogleCalendarsBlanc.setText(MainApp.userPrefs.googleCalendarsBlanc);
        
        cbxAutoComputeHc.setSelected(MainApp.userPrefs.autoComputeCreditHours);
    }

    public void refreshFields(String eventType, String calendars) {
        switch (eventType) {
            case SyncGoogleCalendar.CALENDAR_FLIGHT:
                tfldGoogleCalendarsFlight.setText(calendars);
                break;
            case SyncGoogleCalendar.CALENDAR_GROUND:
                tfldGoogleCalendarsGround.setText(calendars);
                break;
            case SyncGoogleCalendar.CALENDAR_OFF:
                tfldGoogleCalendarsOff.setText(calendars);
                break;
            case SyncGoogleCalendar.CALENDAR_VACATION:
                tfldGoogleCalendarsVacation.setText(calendars);
                break;
            case SyncGoogleCalendar.CALENDAR_BLANC:
                tfldGoogleCalendarsBlanc.setText(calendars);
                break;
            case SyncGoogleCalendar.CALENDAR_EVENTS_DELETE:
                tfldDeleteEventCalendars.setText(calendars);
                break;
            default:
        }
    }

    private boolean showUserCalendarsView(String eventType) {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/UserCalendarsView.fxml"));
            VBox vBox = (VBox) loader.load();
            // Create the dialog Stage.
            Stage stage = new Stage();
            stage.setTitle("Calendrier(s) disponible(s)");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(dialogStage);
            Scene scene = new Scene(vBox);
            stage.setScene(scene);

            // Set the data into the controller.
            UserCalendarsViewController controller = loader.getController();
            controller.initData(eventType);
            controller.setParentController(this);
            controller.setDialogStage(stage);

            // Show the dialog and wait until the user closes it
            stage.showAndWait();

            return controller.isOkClicked();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}
