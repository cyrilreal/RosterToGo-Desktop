/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.gui;

import com.pluszero.rostertogo.ActivityAnalyser;
import com.pluszero.rostertogo.ExportTable;
import com.pluszero.rostertogo.MainApp;
import com.pluszero.rostertogo.model.DutyPeriod;
import com.pluszero.rostertogo.model.PlanningEvent;
import com.pluszero.rostertogo.model.Rotation;
import java.text.SimpleDateFormat;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 *
 * @author Cyril
 */
public class ActivityPayViewController {
    // Reference to the main application

    private final boolean okClicked = false;
    private Stage dialogStage;
    //private ArrayList<Rotation> list;

    private ActivityAnalyser activityAnalyser, activityAnalyserAF;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    private final ObservableList<String> function = FXCollections.observableArrayList("OPL", "CDB");
    private final ObservableList<String> years = FXCollections.observableArrayList("Année 0", "Année 1", "Année 2", "Année 3", "Année 4", "Année 5", "Année 6", "Année 7", "Année 8", "Année 9", "Année 10", "Année 11", "Année 12", "Année 13", "Année 14", "Année 15", "Année 16", "Année 17", "Année 18", "Année 19", "Année 20");
    private final ObservableList<String> classes = FXCollections.observableArrayList("1ère classe", "2ème classe", "3ème classe", "4ème classe", "5ème classe");
    private final ObservableList<String> echelons = FXCollections.observableArrayList("Echelon 1", "Echelon 2", "Echelon 3", "Echelon 4", "Echelon 5", "Echelon 6", "Echelon 7", "Echelon 8", "Echelon 9", "Echelon 10");
    private final ObservableList<String> categories = FXCollections.observableArrayList("Catégorie A", "Catégorie B", "Catégorie C");

    @FXML
    private TabPane tabPane;
    @FXML
    private Button btnClose;
    @FXML
    private TextArea taContent, taContentAF;
    @FXML
    private ComboBox<String> cmbxFunction, cmbxClass, cmbxEchelon, cmbxCategory, cmbxYears;
    @FXML
    private CheckBox chbxAtpl;
    @FXML
    private TextField tfPveiAF, tfFixedPayAF, tfMinimumGuaranteedPayAF, tfFixedPayTO, tfPvTO, tfPvSup75hcTO, tfMinimumGuaranteedPayTO;

    public ActivityPayViewController() {

    }

    /**
     * Sets the stage of this dialog.
     *
     * @param dialogStage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Sets the Activity Analyser for this dialog
     *
     * @param activityAnalyser
     * @param activityAnalyserAF
     */
    public void initData(ActivityAnalyser activityAnalyser, ActivityAnalyser activityAnalyserAF) {
        cmbxYears.setItems(years);
        cmbxYears.setValue(years.get(0));
        cmbxFunction.setItems(function);
        cmbxFunction.getSelectionModel().select(MainApp.userPrefs.crewFunction - 3);
        cmbxCategory.setItems(categories);
        cmbxCategory.setValue(categories.get(0));
        cmbxEchelon.setItems(echelons);
        cmbxEchelon.setValue(echelons.get(0));
        cmbxClass.setItems(classes);
        cmbxClass.setValue(classes.get(4));

        loadPrefs();
        // category and ATPL are for OPL only
        if (cmbxFunction.getSelectionModel().getSelectedIndex() > 0) {
            cmbxCategory.setDisable(true);
            chbxAtpl.setDisable(true);
        } else {
            cmbxCategory.setDisable(false);
            chbxAtpl.setDisable(false);
        }

        this.activityAnalyser = activityAnalyser;
        this.activityAnalyserAF = activityAnalyserAF;

        // pay TO
        activityAnalyser.computeMinimumGuaranteedPayTO(
                cmbxFunction.getSelectionModel().getSelectedIndex() + 3, // FO = 3, Capt = 4, TRI = 5...
                cmbxCategory.getSelectionModel().getSelectedIndex() + 1, // category A = 1...
                cmbxYears.getSelectionModel().getSelectedIndex());
        tfFixedPayTO.setText(String.format("%.2f€", activityAnalyser.fixedPayTO));
        tfPvTO.setText(String.format("%.2f€", activityAnalyser.pvNormTO));
        tfPvSup75hcTO.setText(String.format("%.2f€", activityAnalyser.pvSup75hcTO));
        tfMinimumGuaranteedPayTO.setText(String.format("%.2f€", activityAnalyser.minimumGuaranteedPayTO));
        activityAnalyser.refreshCreditHoursAndPV(ActivityAnalyser.PAY_MODE_TO);
        taContent.setText(displayRotationsList());

        // pay AF
        refreshPayOptionB();
    }

    /**
     * Returns true if the user clicked OK, false otherwise.
     *
     * @return
     */
    public boolean isOkClicked() {
        return okClicked;
    }

    @FXML
    private void actionBtnClose(ActionEvent event) {
        savePrefs();
        dialogStage.close();
    }

    private String displayRotationsList() {

        StringBuilder sb = new StringBuilder();
        sb.append("Option A (ancien système de rémuneration Transavia France)");
        sb.append(System.lineSeparator()).append(System.lineSeparator());

        sb.append(activityAnalyser.buildHoursSheetTO());
        sb.append(System.lineSeparator());

        for (Rotation rotation : activityAnalyser.getAlRotations()) {
            sb.append("ROTATION ").append(sdf.format(rotation.getDateStart())).append(System.lineSeparator());
            for (DutyPeriod dutyPeriod : rotation.getDutyPeriods()) {
                sb.append("\tDuty Period: ");
                sb.append(sdf.format(dutyPeriod.getEvents().get(0).getGcBegin().getTimeInMillis()))
                        .append(" ");
                for (PlanningEvent pe : dutyPeriod.getEvents()) {
                    sb.append(pe.getIataOrig())
                            .append("-")
                            .append(pe.getIataDest())
                            .append(" | ");
                }
                sb.delete(sb.lastIndexOf(" | "), sb.length() - 1);
                sb.append(System.lineSeparator());
                sb.append("\t");
                sb.append("HV(r):").append(String.format("%.2f", dutyPeriod.mSumHV_r))
                        .append(" cmt:").append(String.format("%.2f", dutyPeriod.cmt))
                        .append(" TSV:").append(String.format("%.2f", dutyPeriod.tsv))
                        .append(" Hct:").append(String.format("%.2f", dutyPeriod.hct))
                        .append(" Hcv:").append(String.format("%.2f", dutyPeriod.hcv))
                        .append(" H1:").append(String.format("%.2f", dutyPeriod.h1));
                sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            sb.append("\t");
            sb.append("HVnuit:").append(String.format("%.2f", rotation.sumHvNuit))
                    .append(" HCnuit:").append(String.format("%.2f", rotation.sumHcNuit));
            sb.append(" Hca:").append(String.format("%.2f", rotation.hca))
                    .append(" \u2211H1:").append(String.format("%.2f", rotation.sumH1))
                    .append(" H2:").append(String.format("%.2f", rotation.h2));
            sb.append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }
        return sb.toString().replace("\t", "  ");
    }

    private String displayRotationsListAF() {
        StringBuilder sb = new StringBuilder();
        sb.append("Option B (nouveau système de rémunération Transavia France)");
        sb.append(System.lineSeparator()).append(System.lineSeparator());

        // month figures
        sb.append(activityAnalyserAF.buildHoursSheetAF());
        sb.append(System.lineSeparator());

        // rotations
        for (Rotation rotation : activityAnalyserAF.getAlRotations()) {
            sb.append("ROTATION ").append(sdf.format(rotation.getDateStart())).append(System.lineSeparator());
            for (DutyPeriod dutyPeriod : rotation.getDutyPeriods()) {
                sb.append("\tDuty Period: ");
                sb.append(sdf.format(dutyPeriod.getEvents().get(0).getGcBegin().getTimeInMillis()))
                        .append(" ");
                for (PlanningEvent pe : dutyPeriod.getEvents()) {
                    sb.append(pe.getIataOrig())
                            .append("-")
                            .append(pe.getIataDest())
                            .append(" | ");
                }
                sb.delete(sb.lastIndexOf(" | "), sb.length() - 1);
                sb.append(System.lineSeparator());
                sb.append("\t");
                sb.append("HV100%:").append(String.format("%.2f", dutyPeriod.mSumHV))
                        .append(" HV100%(r):").append(String.format("%.2f", dutyPeriod.mSumHV_r))
                        .append(" cmt:").append(String.format("%.2f", dutyPeriod.cmt))
                        .append(" TSV:").append(String.format("%.2f", dutyPeriod.tsv))
                        .append(" TSVnuit:").append(String.format("%.2f", dutyPeriod.tsvNuit))
                        .append(" Hct:").append(String.format("%.2f", dutyPeriod.hct))
                        .append(" Hcv:").append(String.format("%.2f", dutyPeriod.hcv))
                        .append(" Hcv(r):").append(String.format("%.2f", dutyPeriod.hcv_r))
                        .append(" H1:").append(String.format("%.2f", dutyPeriod.h1))
                        .append(" H1(r):").append(String.format("%.2f", dutyPeriod.h1_r))
                        .append(" PV nuit:").append(String.format("%.2f", dutyPeriod.pvNuit));

                sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            sb.append("\t");
            sb.append("\u2211TSV nuit:").append(String.format("%.2f", rotation.sumTsvNuit))
                    .append(" Hca:").append(String.format("%.2f", rotation.hca))
                    .append(" \u2211H1:").append(String.format("%.2f", rotation.sumH1))
                    .append(" \u2211H1(r):").append(String.format("%.2f", rotation.sumH1_r))
                    .append(" H2:").append(String.format("%.2f", rotation.h2))
                    .append(" H2(r):").append(String.format("%.2f", rotation.h2_r))
                    .append(" PV rm:").append(String.format("%.2f", rotation.pvRm))
                    .append(" PV Nuit:").append(String.format("%.2f", rotation.pvNuit));

            sb.append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }
        return sb.toString().replace("\t", "  ");
    }

    @FXML
    private void actionFunctionChanged(ActionEvent event) {
        activityAnalyser.computeMinimumGuaranteedPayTO(
                cmbxFunction.getSelectionModel().getSelectedIndex() + 3,
                cmbxCategory.getSelectionModel().getSelectedIndex() + 1,
                cmbxYears.getSelectionModel().getSelectedIndex());
        tfFixedPayTO.setText(String.format("%.2f€", activityAnalyser.fixedPayTO));
        tfPvTO.setText(String.format("%.2f€", activityAnalyser.pvNormTO));
        tfPvSup75hcTO.setText(String.format("%.2f€", activityAnalyser.pvSup75hcTO));
        tfMinimumGuaranteedPayTO.setText(String.format("%.2f€", activityAnalyser.minimumGuaranteedPayTO));
        activityAnalyser.refreshCreditHoursAndPV(ActivityAnalyser.PAY_MODE_TO);
        taContent.setText(displayRotationsList());

        refreshPayOptionB();

        // category and atpl are for OPL only
        if (cmbxFunction.getValue().equals("CDB")) {
            cmbxCategory.setDisable(true);
            chbxAtpl.setDisable(true);
        } else {
            cmbxCategory.setDisable(false);
            chbxAtpl.setDisable(false);
        }

    }

    @FXML
    private void actionCategoryChanged(ActionEvent event) {
        activityAnalyser.computeMinimumGuaranteedPayTO(
                cmbxFunction.getSelectionModel().getSelectedIndex() + 3,
                cmbxCategory.getSelectionModel().getSelectedIndex() + 1,
                cmbxYears.getSelectionModel().getSelectedIndex());
        tfFixedPayTO.setText(String.format("%.2f€", activityAnalyser.fixedPayTO));
        tfPvTO.setText(String.format("%.2f€", activityAnalyser.pvNormTO));
        tfPvSup75hcTO.setText(String.format("%.2f€", activityAnalyser.pvSup75hcTO));
        tfMinimumGuaranteedPayTO.setText(String.format("%.2f€", activityAnalyser.minimumGuaranteedPayTO));
        activityAnalyser.refreshCreditHoursAndPV(ActivityAnalyser.PAY_MODE_TO);
        taContent.setText(displayRotationsList());

        refreshPayOptionB();
    }

    @FXML
    private void actionEchelonChanged(ActionEvent event) {
        refreshPayOptionB();
    }

    @FXML
    private void actionClassChanged(ActionEvent event) {
        refreshPayOptionB();
    }

    @FXML
    private void actionAtplChanged(ActionEvent event) {
        refreshPayOptionB();
    }

    @FXML
    private void actionYearChanged(ActionEvent event) {
        activityAnalyser.computeMinimumGuaranteedPayTO(
                cmbxFunction.getSelectionModel().getSelectedIndex() + 3,
                cmbxCategory.getSelectionModel().getSelectedIndex() + 1,
                cmbxYears.getSelectionModel().getSelectedIndex());
        tfFixedPayTO.setText(String.format("%.2f€", activityAnalyser.fixedPayTO));
        tfPvTO.setText(String.format("%.2f€", activityAnalyser.pvNormTO));
        tfPvSup75hcTO.setText(String.format("%.2f€", activityAnalyser.pvSup75hcTO));
        tfMinimumGuaranteedPayTO.setText(String.format("%.2f€", activityAnalyser.minimumGuaranteedPayTO));
        activityAnalyser.refreshCreditHoursAndPV(ActivityAnalyser.PAY_MODE_TO);
        taContent.setText(displayRotationsList());
    }

    @FXML
    private void actionGenerateMonthsTable(MouseEvent event) {
        if (event.isControlDown() && event.isShiftDown()) {
            new ExportTable(activityAnalyser, activityAnalyserAF);
            SimpleDialog.NewInstance("Information", "Les données mensuelles ont été copiées dans le presse papier");
        }
    }

    private void refreshPayOptionB() {
        activityAnalyserAF.computeValuePVEI(
                cmbxFunction.getSelectionModel().getSelectedIndex() + 3,
                cmbxClass.getSelectionModel().getSelectedIndex() + 1,
                cmbxCategory.getSelectionModel().getSelectedIndex() + 1,
                chbxAtpl.isSelected());

        activityAnalyserAF.computeFixedPayAF(
                cmbxFunction.getSelectionModel().getSelectedIndex() + 3,
                cmbxEchelon.getSelectionModel().getSelectedIndex() + 1);

        tfFixedPayAF.setText(String.format("%.2f€", activityAnalyserAF.fixedPayAF));
        activityAnalyserAF.computeMinimumGuaranteedPayAF();
        tfMinimumGuaranteedPayAF.setText(String.format("%.2f€", activityAnalyserAF.minimumGuaranteedPayAF));
        activityAnalyserAF.refreshCreditHoursAndPV(ActivityAnalyser.PAY_MODE_AF);
        taContentAF.setText(displayRotationsListAF());
        tfPveiAF.setText(String.format("%.2f€", activityAnalyserAF.pvei));
    }

    private void loadPrefs() {
        tabPane.getSelectionModel().select(MainApp.userPrefs.crewOption);
        cmbxCategory.getSelectionModel().select(MainApp.userPrefs.crewCategory);
        cmbxYears.getSelectionModel().select(MainApp.userPrefs.crewYearsTO);
        cmbxEchelon.getSelectionModel().select(MainApp.userPrefs.crewEchelon - 1);  // cause echelons start at 1
        cmbxClass.getSelectionModel().select(MainApp.userPrefs.crewClasse - 1);     // cause classes start at 1
        chbxAtpl.setSelected(MainApp.userPrefs.crewAtpl);
    }

    private void savePrefs() {
        MainApp.userPrefs.crewOption = tabPane.getSelectionModel().getSelectedIndex();
        MainApp.userPrefs.crewCategory = cmbxCategory.getSelectionModel().getSelectedIndex();
        MainApp.userPrefs.crewYearsTO = cmbxYears.getSelectionModel().getSelectedIndex();
        MainApp.userPrefs.crewEchelon = cmbxEchelon.getSelectionModel().getSelectedIndex() + 1; // cause echelons start at 1
        MainApp.userPrefs.crewClasse = cmbxClass.getSelectionModel().getSelectedIndex() + 1;    // cause classes start at 1
        MainApp.userPrefs.crewAtpl = chbxAtpl.isSelected();
    }
}
