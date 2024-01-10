package com.pluszero.rostertogo.gui;

import com.pluszero.rostertogo.DateComparator;
import com.pluszero.rostertogo.ExportTable;
import com.pluszero.rostertogo.ICSEvent;
import com.pluszero.rostertogo.IcsWriter;
import com.pluszero.rostertogo.MainApp;
import com.pluszero.rostertogo.Utils;
import com.pluszero.rostertogo.online.UpdateChecker;
import com.pluszero.rostertogo.online.SyncGoogleCalendar;
import com.pluszero.rostertogo.model.PlanningModel;
import com.pluszero.rostertogo.model.PlanningEvent;
import com.pluszero.rostertogo.online.HttpClientCrewWebPlus;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Scanner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 * @author Cyril
 */
public class MainViewController implements Initializable {

    // Reference to the main application
    private MainApp mainApp;

    @FXML
    private Button btnLogin;
    @FXML
    private ProgressBar pbProgress;
    @FXML
    private Label labStatus;
    @FXML
    private Label labVersion;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnLoad;
    @FXML
    private Button btnOptions;
    @FXML
    private Button btnGoogle;
    @FXML
    private Button btnActivityPay;
    @FXML
    private TextArea txtaActivityFigures;
    @FXML
    private ListView<PlanningEvent> lvPlanning;

    @FXML
    private ComboBox<String> cmbxTimeZone;
    @FXML
    private Label labBlockHours;

    @FXML
    private WebView webView;

    private HttpClientCrewWebPlus clientCrewWebPlus;
    private String sessionId;

    private PlanningModel plngModel;
    private FileChooser fileChooser;
    private final ObservableList<String> timeZones = FXCollections.observableArrayList("Europe/Paris", "UTC");
    private ArrayList<String> airports;
    private UpdateChecker updateChecker;

    public static final String VERSION_NUMBER = "v1.1 Build";
    public static final int BUILD_NUMBER = 202309011; // publication date yyyymmdd

    public String applicationDirectory;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        labVersion.setText(VERSION_NUMBER + " " + BUILD_NUMBER);
        // check for updates
        checkForUpdate();

        cmbxTimeZone.setItems(timeZones);
        cmbxTimeZone.setValue(timeZones.get(0));
        airports = buildAirportsDirectory();

        txtaActivityFigures.setStyle("-fx-font-family: \"Monospace\";");
    }

    @FXML
    private void actionBtnConnect(MouseEvent event) {
        labStatus.setText("Ouverture de la fenêtre de login");
        showLoginView();
    }

    @FXML
    private void actionTimeRefChanged(ActionEvent event) {
        if (plngModel == null || plngModel.getAlEvents().isEmpty()) {
            return;
        }
        displayPlanning();
    }

    @FXML
    private void actionBtnSave(ActionEvent event) {

        if (plngModel == null || plngModel.getAlEvents().isEmpty()) {
            SimpleDialog.NewInstance("Attention", "Pas de planning à sauvegarder");
            return;
        }

        if (!plngModel.modeOnline) {
            SimpleDialog.NewInstance("Attention", "Pas de nouveau planning à sauvegarder");
            return;
        }
        File file;
        int flagIcs = -1;
        int flagPdf = -1;

        String filesSavingLocation = MainApp.userPrefs.filesSavingLocation;
        Path path = Paths.get(filesSavingLocation);
        if (!Files.exists(path) || "".equals(filesSavingLocation)) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Dossier cible pour les fichiers ics et pdf");
            File defaultDirectory = new File(System.getProperty("user.home"));
            chooser.setInitialDirectory(defaultDirectory);
            File selectedDirectory = chooser.showDialog(btnSave.getScene().getWindow());

            if (selectedDirectory == null) {
                return;
            }
            filesSavingLocation = selectedDirectory.getAbsolutePath();
        }
        //save ics
        String filename = "/tvf_plng_" + clientCrewWebPlus.strPlanningFilesDate + ".ics";

        if (!"".equals(filesSavingLocation) && filesSavingLocation != null) {
            file = new File(filesSavingLocation + filename);
            IcsWriter writer = new IcsWriter(plngModel);
            flagIcs = Utils.saveFile(writer.getContent(), file);
        }

        // save pdf
        filename = "/tvf_plng_" + clientCrewWebPlus.strPlanningFilesDate + ".pdf";

        if (!"".equals(filesSavingLocation) && filesSavingLocation != null) {
            file = new File(filesSavingLocation + filename);
//            if (connectTo.contentPdf != null && !"".equals(connectTo.contentPdf)) {
//                flagPdf = Utils.saveFile(connectTo.contentPdf, file);
//            }
            if (clientCrewWebPlus.contentPdf != null && !"".equals(clientCrewWebPlus.contentPdf)) {
                flagPdf = Utils.saveFile(clientCrewWebPlus.contentPdf, file);
            }
        }

        // inform user
        StringBuilder sb = new StringBuilder();
        sb.append("Sauvegarde du fichier .ics : ").append(flagIcs)
                .append(System.lineSeparator())
                .append("Sauvegarde du fichier .pdf : ").append(flagPdf);
        String msg = sb.toString().replace("0", "OK").replace("-1", "Erreur");

        SimpleDialog.NewInstance("Information", msg);
    }

    @FXML
    private void actionBtnLoad(MouseEvent evt) {
        // handle cheat mode to delete current planning model
        if (evt.isControlDown() && evt.isShiftDown()) {
            resetPlanning();
            return;
        }

        fileChooser = new FileChooser();
        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Calendar files (*.ics)", "*.ics");
        fileChooser.getExtensionFilters().add(extFilter);
        String latest = MainApp.userPrefs.latestLoadLocation;
        if (!latest.equals("")) {
            Path dir = Paths.get(latest);
            if (Files.exists(dir)) {
                fileChooser.setInitialDirectory(new File(latest));
            } else {
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            }
        }

        // handle multiple files
        List<File> filesList = fileChooser.showOpenMultipleDialog(btnSave.getScene().getWindow());

        if (filesList != null && !filesList.isEmpty()) {
            // Remember latest directory and save it in prefs
            MainApp.userPrefs.latestLoadLocation = filesList.get(0).getParent();
            MainApp.userPrefs.savePrefs();

            if (plngModel == null) {
                plngModel = new PlanningModel(airports);
            }
            plngModel.modeOnline = false;
            // sort alphabetically (mandatory, otherwise it may bug if user has
            // set file order by name descending in fileChooser)
            File[] files = new File[filesList.size()];
            for (int i = 0; i < filesList.size(); i++) {
                files[i] = filesList.get(i);
            }
            Arrays.sort(files);
            for (File f : files) {
                buildPlanning(f);
            }
            plngModel.computeActivityFigures();
            displayPlanning();
            displayHours();
        }
    }

    @FXML
    private void actionBtnOptions(ActionEvent event) {
        showOptionsView();
    }

    @FXML
    private void actionBtnGoogle(ActionEvent event) {

        if (lvPlanning.getItems().isEmpty()) {
            SimpleDialog.NewInstance("Attention", "Rien à exporter");
            return;
        }

        if (!checkTargetCalendarsNotVoid()) {
            SimpleDialog.NewInstance("Attention", "Aucun calendrier défini dans les options\n\nAu moins un des types d'évenements doit contenir\nun agenda de destination au minimum");
            return;
        }

        SyncGoogleCalendar sgc = new SyncGoogleCalendar();
        pbProgress.setVisible(true);
        //labStatus.setVisible(true);
        labStatus.textProperty().bind(sgc.messageProperty());

        sgc.setModel(plngModel);
        sgc.setMode(SyncGoogleCalendar.MODE_FULL_EXPORT);
        btnGoogle.setDisable(true);
        sgc.setOnSucceeded((WorkerStateEvent ev) -> {
            pbProgress.setVisible(false);
            //labStatus.setVisible(false);
            labStatus.textProperty().unbind();
            labStatus.setText("");
            btnGoogle.setDisable(false);
            if (!ev.getSource().getValue().equals(SyncGoogleCalendar.MSG_SYNC_OK)) {
                SimpleDialog.NewInstance("Attention", ev.getSource().getMessage());
            } else {
                SimpleDialog.NewInstance("Information", "Synchronisation avec Google Agenda réussie");
            }
        });
        sgc.setOnFailed((WorkerStateEvent ev) -> {
            pbProgress.setVisible(false);
            //labStatus.setVisible(false);
            labStatus.textProperty().unbind();
            labStatus.setText("");
            btnGoogle.setDisable(false);
            SimpleDialog.NewInstance("Attention", ev.getSource().getMessage());
        });
        new Thread(sgc).start();
    }

    @FXML
    private void actionBtnActivityPay(MouseEvent event) {

        if (plngModel != null && plngModel.getAlEvents().size() > 0) {
            if (event.isControlDown() && !event.isShiftDown()) {
                new ExportTable(plngModel.getAlEvents());
                SimpleDialog.NewInstance("Information", "Les données de planning ont été copiées dans le presse papier");
            } else {
                if (MainApp.userPrefs.isPilot()) {
                    showRotationsView();
                } else {
                    SimpleDialog.NewInstance("Information", "Fonctionnalité accessible uniquement aux pilotes\n(pour le moment)");
                }
            }
        } else {
            SimpleDialog.NewInstance("Attention", "Aucune donnée à copier !");
        }
    }

    private ArrayList<PlanningEvent> extractICS(String content) {
        ArrayList<PlanningEvent> alEvents = new ArrayList();
        // Nombre total d'évènements
        int nbEvents = 0;
        int currentEvent = 1;
        //planningModel.getAlEvents().clear();

        // Extraction planning
        if (!content.equals("")) {
            int indexICS = 0;
            boolean firstEvent = true;

            while ((indexICS = content.indexOf(ICSEvent.TAG_BEGIN, indexICS)) != -1) {

                String source = Utils.extractString(content, ICSEvent.TAG_BEGIN, ICSEvent.TAG_END, indexICS);
                ICSEvent event = new ICSEvent(source);
                PlanningEvent planningEvent = new PlanningEvent(event.getICSStart(), event.getICSEnd(), event.getICSSummary(), event.getICSCategory(), event.getICSDesc()); // Incrément pour parcourir le fichier
                // do not check status in offline mode as it is not saved with IcsWriter (backward compatibility)
                if (!event.getICSStatus().equals("CANCELLED") && !event.getICSCategory().equals("ROTATION")) {
                    alEvents.add(planningEvent);
                }
                indexICS += source.length();
            }
            Collections.sort(alEvents, new DateComparator());
        }
        return alEvents;
    }

    private void displayPlanning() {

        ObservableList items = FXCollections.observableArrayList(plngModel.getAlEvents());
        lvPlanning.setItems(items);

        lvPlanning.setCellFactory((ListView<PlanningEvent> p) -> new EventCell(cmbxTimeZone.getValue()));
    }

    private void displayHours() {
        if (plngModel.getActivityAnalyser() != null) {
            if (MainApp.userPrefs.isPilot()) {
                txtaActivityFigures.setText(plngModel.getActivityAnalyserAF().buildSimpleHoursSheetAF());
            } else {
                txtaActivityFigures.setText(plngModel.getActivityAnalyser().buildSimpleHoursSheetTO());
            }
        } else {
            txtaActivityFigures.setText("");
        }
    }

    /**
     * Is called by the main application to give a reference back to itself.
     *
     * @param mainApp
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;

    }

    private boolean showOptionsView() {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loaderOptions = new FXMLLoader();
            loaderOptions
                    .setLocation(MainApp.class
                            .getResource("/fxml/OptionsView.fxml"));
            BorderPane page = (BorderPane) loaderOptions.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Options");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(mainApp.getPrimaryStage());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            // Set the person into the controller.
            OptionsViewController controller = loaderOptions.getController();
            controller.setDialogStage(dialogStage);

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();

            return controller.isOkClicked();
        } catch (IOException e) {
            return false;
        }
    }

    private boolean showCrewWebPlusConnection() {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loaderOptions = new FXMLLoader();
            loaderOptions
                    .setLocation(MainApp.class
                            .getResource("/fxml/Scene.fxml"));
            BorderPane page = (BorderPane) loaderOptions.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Scene");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(mainApp.getPrimaryStage());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            webEngine.load("https://www.google.com");
            webEngine.getDocument();

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean showRotationsView() {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loaderOptions = new FXMLLoader();
            loaderOptions.setLocation(MainApp.class
                    .getResource("/fxml/ActivityPayView.fxml"));
            BorderPane page = (BorderPane) loaderOptions.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Rotations");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(mainApp.getPrimaryStage());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            // Set the person into the controller.
            ActivityPayViewController controller = loaderOptions.getController();
            controller.setDialogStage(dialogStage);
            controller.initData(plngModel.getActivityAnalyser(), plngModel.getActivityAnalyserAF());

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();

            return controller.isOkClicked();
        } catch (IOException e) {
            return false;
        }
    }

    private boolean showLoginView() {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loaderOptions = new FXMLLoader();
            loaderOptions
                    .setLocation(MainApp.class
                            .getResource("/fxml/LoginView.fxml"));
            BorderPane page = (BorderPane) loaderOptions.load();

            // Create the dialog Stage.
            Stage loginStage = new Stage();
            loginStage.setTitle("Okta Login");
            loginStage.initModality(Modality.WINDOW_MODAL);
            loginStage.initOwner(mainApp.getPrimaryStage());
            Scene scene = new Scene(page);
            loginStage.setScene(scene);
            // Set the person into the controller.
            LoginViewController loginController = loaderOptions.getController();
            loginController.setMainViewController(this);
            loginController.setStage(loginStage);
            loginController.loadPrefs();
            loginStage.setOnHidden(e -> loginController.savePrefs());

            // Show the dialog and wait until the user closes it
            loginStage.showAndWait();

        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private ArrayList<String> buildAirportsDirectory() {
        ArrayList<String> list = new ArrayList();
        // Load airport directory from file.
        Scanner scanner;
        InputStream is = this.getClass().getResourceAsStream("/data/airports.dat");
        BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String[] result;
        scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            list.add(line);
        }
        scanner.close();
        return list;
    }

    private boolean checkTargetCalendarsNotVoid() {
        boolean flag = false;

        if (!MainApp.userPrefs.googleCalendarsFlight.equals("")) {
            flag = true;
        }
        if (!MainApp.userPrefs.googleCalendarsGround.equals("")) {
            flag = true;
        }
        if (!MainApp.userPrefs.googleCalendarsOff.equals("")) {
            flag = true;
        }
        if (!MainApp.userPrefs.googleCalendarsVacation.equals("")) {
            flag = true;
        }
        if (!MainApp.userPrefs.googleCalendarsBlanc.equals("")) {
            flag = true;
        }
        return flag;
    }

    private void addToModel(ArrayList<PlanningEvent> list) {
        ArrayList<PlanningEvent> events = new ArrayList<>();
        // get the date of first event, set it at 00h00
        GregorianCalendar c = new GregorianCalendar();
        //TODO: random bugs when list is null or empty 
        if (list == null || list.isEmpty()) {
            SimpleDialog.NewInstance(
                    "Attention",
                    "Le planning n'a pas pu être récupéré correctement\nVeuillez recommencer");
        }
        c.setTime(list.get(0).getGcBegin().getTime());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        //add events of actual model that are before, in a new arraylist
        for (PlanningEvent pe : plngModel.getAlEvents()) {
            if (pe.getGcBegin().getTimeInMillis() < c.getTimeInMillis()) {
                events.add(pe);
            }
        }
        events.addAll(list);
        // add content of new list to model
        plngModel.getAlEvents().clear();
        plngModel.getAlEvents().addAll(events);
        plngModel.sortByAscendingDate();
    }

    private void buildPlanning(File file) {
        if (plngModel.modeOnline) {
            addToModel(extractICS(clientCrewWebPlus.contentIcs));
        } else {
            addToModel(extractICS(Utils.readFile(file).toString()));
        }

        // make some modifications 
        plngModel.findAirportDetails();
        plngModel.copyCrew();
        plngModel.fixSplittedActivities();
        if (plngModel.modeOnline) {
            plngModel.addJoursBlanc();
        }
        plngModel.factorizeDays();

        // get data from PDF
        // IMPORTANT : add data from PDF after previous work
        // to prevent data modifications (decoded crew names)
        if (!MainApp.userPrefs.usePdfData) {
            return;
        }

        try {
            if (plngModel.modeOnline) {
                plngModel.addDataFromPDF(null, clientCrewWebPlus.contentPdf);
            } else {
                plngModel.addDataFromPDF(file, null);
            }
            for (PlanningEvent pe : plngModel.getAlEvents()) {
                System.out.println(new SimpleDateFormat().format(
                        pe.getGcBegin().getTime()) + " " + pe.getCategory()
                        + " " + pe.getSummary());
                System.out.println(Arrays.toString(pe.getCrewTec()));
            }
        } catch (Exception e) {
            if (file != null) {
                SimpleDialog.NewInstance(
                        "Attention",
                        "Les données additionelles n'ont pas pu être exploitées correctement."
                        + "\nVous pouvez envoyer les fichiers suivant au développeur pour investigation :\n\n"
                        + file.getName() + "\n" + file.getName().replace(".ics", ".pdf"));
            } else {
                SimpleDialog.NewInstance(
                        "Attention",
                        "Les données additionelles n'ont pas pu être exploitées correctement.");
            }
        }
    }

    private void checkForUpdate() {
        updateChecker = new UpdateChecker();
        updateChecker.setOnSucceeded((WorkerStateEvent ev) -> {
            int result = (int) ev.getSource().getValue();
            if (result == UpdateChecker.MSG_CONNECTION_ERROR) {
                SimpleDialog.NewInstance(
                        "Attention",
                        "Vérification automatique de la mise à jour indisponible");
            }

            if (BUILD_NUMBER < result) {
                UpdateDialog.NewInstance();
            }
        });
        new Thread(updateChecker).start();
    }

    public HttpClientCrewWebPlus getClientCrewWebPlus() {
        return clientCrewWebPlus;
    }

    private void resetPlanning() {
        Alert alertConfirm = new Alert(Alert.AlertType.WARNING);
        alertConfirm.setTitle("Attention !");
        alertConfirm.setHeaderText(null);
        alertConfirm.setContentText("Voulez vous vraiment réinitialiser le planning actuel ?");
        Optional<ButtonType> result = alertConfirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            plngModel = new PlanningModel(airports);
            displayPlanning();
            displayHours();
        }
    }

    public void launchConnectionWithToken() {
        clientCrewWebPlus = new HttpClientCrewWebPlus(sessionId);
        pbProgress.setVisible(true);
        //labStatus.setVisible(true);
        labStatus.textProperty().bind(clientCrewWebPlus.messageProperty());

        clientCrewWebPlus.setOnSucceeded((WorkerStateEvent ev) -> {

            if (ev.getSource().getValue().equals(HttpClientCrewWebPlus.MSG_PROCESS_FINISHED) || ev.getSource().getValue().equals(HttpClientCrewWebPlus.MSG_PROCESS_FINISHED_WITH_CHANGES_OR_SIGNED) || ev.getSource().getValue().equals(HttpClientCrewWebPlus.MSG_PDF_NOT_SAVE)) {
                plngModel = new PlanningModel(airports);
                plngModel.modeOnline = true;
                buildPlanning(null);
                plngModel.computeActivityFigures();
                displayPlanning();
                displayHours();
            } else {
                SimpleDialog.NewInstance("Attention", ev.getSource().getValue().toString());
                return;
            }
            btnLogin.setDisable(false);
            pbProgress.setVisible(false);
            labStatus.textProperty().unbind();
            labStatus.setText("");
        });

        clientCrewWebPlus.setOnFailed((WorkerStateEvent ev) -> {
            SimpleDialog.NewInstance("Attention", ev.getSource().getValue().toString());
            //progress.getDialogStage().close();
            btnLogin.setDisable(false);
            pbProgress.setVisible(false);
            labStatus.textProperty().unbind();
            labStatus.setText("");
            //labStatus.setVisible(false);
        });

        new Thread(clientCrewWebPlus).start();
        btnLogin.setDisable(true);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

}
