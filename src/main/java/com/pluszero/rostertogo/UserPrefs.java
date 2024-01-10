/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo;

import java.util.prefs.Preferences;

/**
 *
 * @author Cyril
 */
public class UserPrefs {

    // registry keys
    private final static String OKTA_LOGIN = "toconnectLogin";
    private final static String OKTA_PASSWORD = "toconnectPassword";
    private final static String NODE_ROSTERTOGO_OPTIONS = "RosterToGo/Options";
    private final static String GOOGLE_LOGIN = "googleLogin";
    private final static String GOOGLE_CALENDARS_FLIGHT = "googleCalendarsFlight";
    private final static String GOOGLE_CALENDARS_GROUND = "googleCalendarsGround";
    private final static String GOOGLE_CALENDARS_OFF = "googleCalendarsOff";
    private final static String GOOGLE_CALENDARS_VACATION = "googleCalendarsVacation";
    private final static String GOOGLE_CALENDARS_BLANC = "googleCalendarsBlanc";
    private final static String LATEST_LOAD_LOCATION = "latestLoadLocation";
    private final static String FILES_SAVING_LOCATION = "filesSavingLocation";
    private final static String GOOGLE_COLORIZE_EVENTS = "googleColorizeEvents";
    private final static String AUTO_CHECK_AND_SIGN = "auto_check_and_sign";
    private final static String USE_PDF_DATA = "use_pdf_data";
    private final static String SHOW_ACCPTD_CREW_RQST = "show_accepted_crew_request";
    private final static String AUTO_COMPUTE_CREDIT_HOURS = "auto_compute_credit_hours";
    private final static String CREW_PROFILE_OPTION = "crew_profile_option";
    private final static String CREW_PROFILE_CATEGORY = "crew_profile_category";
    private final static String CREW_PROFILE_YEARS = "crew_profile_years";
    private final static String CREW_PROFILE_ECHELON = "crew_profile_echelon";
    private final static String CREW_PROFILE_CLASS = "crew_profile_classe";
    private final static String CREW_PROFILE_ATPL = "crew_profile_atpl";

    public static final int FUNCTION_UNKNOWN = -1;
    public static final int FUNCTION_TRI = 5;
    public static final int FUNCTION_CPT = 4;
    public static final int FUNCTION_FO = 3;

    public static final int CATEGORY_A = 1;
    public static final int CATEGORY_B = 2;
    public static final int CATEGORY_C = 3;

    private final Preferences prefs;
    public boolean autoCheckAndSign;
    public boolean usePdfData;
    public boolean showAcceptedCrewRequest;
    public String oktaLogin = "";
    public String oktaPassword = "";
    public String googleLogin = "";
    public boolean googleColorizeEvents;
    public String googleCalendarsFlight = "";
    public String googleCalendarsGround = "";
    public String googleCalendarsOff = "";
    public String googleCalendarsVacation = "";
    public String googleCalendarsBlanc = "";
    public String latestLoadLocation = "";
    public String filesSavingLocation = "";
    public Boolean autoComputeCreditHours;

    public int crewOption;
    public int crewFunction;
    public int crewCategory;
    public int crewEchelon;
    public int crewClasse;
    public boolean crewAtpl;
    public int crewCategoryTO;
    public int crewYearsTO;

    public UserPrefs() {
        prefs = Preferences.userRoot().node(NODE_ROSTERTOGO_OPTIONS);
        loadPrefs();
    }

    private void loadPrefs() {
        oktaLogin = prefs.get(OKTA_LOGIN, "");
        oktaPassword = prefs.get(OKTA_PASSWORD, "");

        autoCheckAndSign = prefs.getBoolean(AUTO_CHECK_AND_SIGN, false);
        usePdfData = prefs.getBoolean(USE_PDF_DATA, true);
        showAcceptedCrewRequest = prefs.getBoolean(SHOW_ACCPTD_CREW_RQST, true);
        googleLogin = prefs.get(GOOGLE_LOGIN, "");
        googleColorizeEvents = prefs.getBoolean(GOOGLE_COLORIZE_EVENTS, true);
        googleCalendarsFlight = prefs.get(GOOGLE_CALENDARS_FLIGHT, "");
        googleCalendarsGround = prefs.get(GOOGLE_CALENDARS_GROUND, "");
        googleCalendarsOff = prefs.get(GOOGLE_CALENDARS_OFF, "");
        googleCalendarsVacation = prefs.get(GOOGLE_CALENDARS_VACATION, "");
        googleCalendarsBlanc = prefs.get(GOOGLE_CALENDARS_BLANC, "");

        latestLoadLocation = prefs.get(LATEST_LOAD_LOCATION, "");
        filesSavingLocation = prefs.get(FILES_SAVING_LOCATION, "");

        autoComputeCreditHours = prefs.getBoolean(AUTO_COMPUTE_CREDIT_HOURS, true);

        crewOption = prefs.getInt(CREW_PROFILE_OPTION, 1);
        crewCategory = prefs.getInt(CREW_PROFILE_CATEGORY, 3);
        crewYearsTO = prefs.getInt(CREW_PROFILE_YEARS, 0);
        crewEchelon = prefs.getInt(CREW_PROFILE_ECHELON, 1);
        crewClasse = prefs.getInt(CREW_PROFILE_CLASS, 5);
        crewAtpl = prefs.getBoolean(CREW_PROFILE_ATPL, false);
    }

    public void savePrefs() {
        prefs.put(OKTA_LOGIN, oktaLogin);
        prefs.put(OKTA_PASSWORD, oktaPassword);

        prefs.putBoolean(AUTO_CHECK_AND_SIGN, autoCheckAndSign);
        prefs.putBoolean(USE_PDF_DATA, usePdfData);
        prefs.putBoolean(SHOW_ACCPTD_CREW_RQST, showAcceptedCrewRequest);
        prefs.put(GOOGLE_LOGIN, googleLogin);
        prefs.putBoolean(GOOGLE_COLORIZE_EVENTS, googleColorizeEvents);
        prefs.put(GOOGLE_CALENDARS_FLIGHT, googleCalendarsFlight);
        prefs.put(GOOGLE_CALENDARS_GROUND, googleCalendarsGround);
        prefs.put(GOOGLE_CALENDARS_OFF, googleCalendarsOff);
        prefs.put(GOOGLE_CALENDARS_VACATION, googleCalendarsVacation);
        prefs.put(GOOGLE_CALENDARS_BLANC, googleCalendarsBlanc);

        prefs.put(LATEST_LOAD_LOCATION, latestLoadLocation);
        prefs.put(FILES_SAVING_LOCATION, filesSavingLocation);

        prefs.putBoolean(AUTO_COMPUTE_CREDIT_HOURS, autoComputeCreditHours);

        prefs.putInt(CREW_PROFILE_OPTION, crewOption);
        prefs.putInt(CREW_PROFILE_CATEGORY, crewCategory);
        prefs.putInt(CREW_PROFILE_YEARS, crewYearsTO);
        prefs.putInt(CREW_PROFILE_ECHELON, crewEchelon);
        prefs.putInt(CREW_PROFILE_CLASS, crewClasse);
        prefs.putBoolean(CREW_PROFILE_ATPL, crewAtpl);
    }

    public static int crewFunctionToInt(String function) {
        switch (function) {
            case "OPL":
                return FUNCTION_FO;
            case "CDB":
                return FUNCTION_CPT;
            case "IPL":
                return FUNCTION_TRI;

            default:
                return FUNCTION_UNKNOWN;
        }
    }

    public boolean isPilot() {
        switch (crewFunction) {
            case FUNCTION_CPT:
                return true;
            case FUNCTION_FO:
                return true;
            case FUNCTION_TRI:
                return true;

            default:
                return false;
        }
    }

    public boolean isCaptain(int i) {
        switch (i) {
            case FUNCTION_CPT:
                return true;
            case FUNCTION_TRI:
                return true;

            default:
                return false;
        }
    }
}
