/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.gui;

import com.pluszero.rostertogo.MainApp;
import com.pluszero.rostertogo.online.SyncGoogleCalendar;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Cyril
 */
public class UserCalendarsViewController implements Initializable {

    private OptionsViewController parentController;
    private boolean okClicked = false;
    private Stage dialogStage;
    private String selectedCalendars;
    private String eventType;

    @FXML
    Label labStatus;
    @FXML
    ListView<String> list;
    @FXML
    Button btnValider;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    public void initData(String eventType) {
        this.eventType = eventType;
        switch (eventType) {
            case SyncGoogleCalendar.CALENDAR_FLIGHT:
                selectedCalendars = MainApp.userPrefs.googleCalendarsFlight;
                break;
            case SyncGoogleCalendar.CALENDAR_GROUND:
                selectedCalendars = MainApp.userPrefs.googleCalendarsGround;
                break;
            case SyncGoogleCalendar.CALENDAR_OFF:
                selectedCalendars = MainApp.userPrefs.googleCalendarsOff;
                break;
            case SyncGoogleCalendar.CALENDAR_VACATION:
                selectedCalendars = MainApp.userPrefs.googleCalendarsVacation;
                break;
            case SyncGoogleCalendar.CALENDAR_BLANC:
                selectedCalendars = MainApp.userPrefs.googleCalendarsBlanc;
                break;
            case SyncGoogleCalendar.CALENDAR_EVENTS_DELETE:
                // do nothing
                break;
            default:
                selectedCalendars = MainApp.userPrefs.googleCalendarsFlight;

        }
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        SyncGoogleCalendar sync = new SyncGoogleCalendar();
        sync.setMode(SyncGoogleCalendar.MODE_FETCH_CALENDARS);
        sync.setOnSucceeded((WorkerStateEvent ev) -> {
            if (ev.getSource().getValue().equals(SyncGoogleCalendar.MSG_CALENDARS_LIST_OK)) {
                if (sync.getCalendarNames() != null) {
                    ObservableList<String> items = FXCollections.observableArrayList(sync.getCalendarNames());
                    list.setItems(items);
                    selectIndicesOfSavedCalendars(selectedCalendars);
                    labStatus.textProperty().unbind();
                    labStatus.setText("Sélectionnez le(s) agenda(s) pour le type d'évenement");
                }
            } else if (ev.getSource().getValue().equals(SyncGoogleCalendar.MSG_GOOGLE_CONNECTION_FAILED)) {
                SimpleDialog.NewInstance("Attention", "Impossible de se connecter à Google");
            } else if (ev.getSource().getValue().equals(SyncGoogleCalendar.MSG_CALENDARS_LIST_FAILED)) {
                SimpleDialog.NewInstance("Attention", "Impossible de srécupérer la liste des agendas");
            } else {
                SimpleDialog.NewInstance("Attention", ev.getSource().getValue().toString());
            }
        });

        sync.setOnFailed((WorkerStateEvent ev) -> {
            if (ev.getSource().getValue().equals(SyncGoogleCalendar.MSG_GOOGLE_CONNECTION_FAILED)) {
                SimpleDialog.NewInstance("Attention", "Impossible de se connecter à Google");
            }
            if (ev.getSource().getValue().equals(SyncGoogleCalendar.MSG_CALENDARS_LIST_FAILED)) {
                SimpleDialog.NewInstance("Attention", "Impossible de srécupérer la liste des agendas");
            }
        });
        new Thread(sync).start();
        labStatus.textProperty().bind(sync.messageProperty());
    }

    public void setParentController(OptionsViewController parentController) {
        this.parentController = parentController;
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
        selectedCalendars = getSelectedCalendars(list.getSelectionModel().getSelectedItems());

        okClicked = true;
        parentController.refreshFields(eventType, selectedCalendars);
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

    private String getSelectedCalendars(ObservableList<String> list) {
        StringBuilder sb = new StringBuilder();

        list.stream().forEach((item) -> {
            sb.append(item).append(" ; ");
        });
        // remove last ";"
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.lastIndexOf(";"));
        }
        return sb.toString().trim();
    }

    private void selectIndicesOfSavedCalendars(String savedCalendars) {
        String[] cals = savedCalendars.split(";");

        ObservableList<String> obsList = list.getItems();
        for (String s : cals) {
            for (int i = 0; i < obsList.size(); i++) {
                if (s.trim().equals(obsList.get(i))) {
                    list.getSelectionModel().selectIndices(i, null);
                }
            }
        }
    }
}
