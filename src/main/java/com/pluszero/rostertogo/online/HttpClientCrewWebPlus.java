/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.pluszero.rostertogo.online;

import com.pluszero.rostertogo.Utils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.concurrent.Task;

/**
 *
 * @author Cyril
 */
public class HttpClientCrewWebPlus extends Task<String> {

    private static final String URL_ICS = "https://planning.to.aero/FlightProgram/GetICS";
    private static final String URL_PDF = "https://planning.to.aero/FlightProgram/GetPdf";
    private static final String URL_PERSONAL_DATA = "https://planning.to.aero/PersonalData";
    private final static String[] PILOT_FUNCTION_IDENT = {"CDB", "OPL"}; 

    public static final String MSG_PROCESS_FINISHED = "Processus terminé";
    public static final String MSG_PROCESS_FINISHED_WITH_CHANGES_OR_SIGNED = "Process finished with changes or signed";
    public static final String MSG_LOGIN_IN_PROGRESS = "Identification en cours";
    public static final String MSG_LOGIN_OK = "Identification OK";
    public static final String MSG_CONNECTING_DASHBOARD = "Connexion au dashboard";
    public static final String MSG_FETCHING_PLANNING = "Récupération du planning";
    public static final String MSG_FETCHING_ADDITIONAL_DATA = "Récupération des données additionnelles";
    public static final String MSG_FETCHING_USER_DATA = "Récupération des données utilisateur";
    public static final String MSG_ROSTER_MODIFICATIONS_NOT_CHECKED = "Modifications de planning non vérifiées";
    public static final String MSG_ROSTER_NOT_SIGNED = "Planning non signé";
    public static final String MSG_LOGIN_PASS_ERROR = "Le login ou le mot de passe saisi est incorrect";
    public static final String MSG_CONNECTION_ERROR = "Erreur durant la connexion";
    public static final String MSG_CHECKING_CHANGES = "Vérification des changements";
    public static final String MSG_PLANNING_VALIDATION = "Validation du planning";
    public static final String MSG_PDF_NOT_SAVE = "Le fichier PDF n'a pu être sauvé";
    public static final String MSG_TIMEOUT = "Le serveur ne répond pas (temps de connexion dépassé)";

    public String contentIcs, contentPdf;

    private String userTrigraph;    // crewmember's userTrigraph

    private Date datePlanningFiles;
    private final SimpleDateFormat sdfPlanningFiles = new SimpleDateFormat("yyyyMMdd_HHmm");
    public String strPlanningFilesDate;

    private final String sessionId;
    private HttpURLConnection huc;
    private URL url;

    
    public HttpClientCrewWebPlus(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserTrigraph() {
        return userTrigraph;
    }

    @Override
    protected String call() throws Exception {
        // create the string-date for file saving
        datePlanningFiles = new Date(System.currentTimeMillis());
        strPlanningFilesDate = sdfPlanningFiles.format(datePlanningFiles);
        // get user function (pilot, cabin attendant...)
        updateMessage(MSG_FETCHING_USER_DATA);
        // get the files
        updateMessage(MSG_FETCHING_PLANNING);
        fetchFile(URL_ICS);
        updateMessage(MSG_FETCHING_ADDITIONAL_DATA);
        fetchFile(URL_PDF);
        return MSG_PROCESS_FINISHED;
    }

    private void fetchFile(String fileUrl) {

        try {
            url = new URL(fileUrl);
            huc = (HttpURLConnection) url.openConnection();
            huc.setRequestProperty("Cookie", this.sessionId);
            huc.setDoOutput(true);
            huc.setDoInput(true);
            huc.setRequestMethod("GET");

            if (fileUrl.equals(URL_ICS)) {
                contentIcs = Utils.stringFromInputStream(huc.getInputStream());
            } else if (fileUrl.equals(URL_PDF)) {
                contentPdf = Utils.stringFromInputStream(huc.getInputStream());
            }
        } catch (MalformedURLException ex) {
        } catch (IOException ex) {
        }
    }
}
