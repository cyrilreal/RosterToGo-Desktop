package com.pluszero.rostertogo.online;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.EventDateTime;

import com.pluszero.rostertogo.MainApp;
import com.pluszero.rostertogo.Utils;
import com.pluszero.rostertogo.model.CrewMember;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javafx.concurrent.Task;
import com.pluszero.rostertogo.model.PlanningEvent;
import com.pluszero.rostertogo.model.PlanningModel;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncGoogleCalendar extends Task<String> {

    public static final int MODE_FULL_EXPORT = 0;
    public static final int MODE_FETCH_CALENDARS = 1;
    public static final int MODE_DELETE_EVENTS = 2;

    public static final String CALENDAR_FLIGHT = "calendar_flight";
    public static final String CALENDAR_GROUND = "calendar_ground";
    public static final String CALENDAR_OFF = "calendar_off";
    public static final String CALENDAR_VACATION = "calendar_vacation";
    public static final String CALENDAR_BLANC = "calendar_blanc";
    public static final String CALENDAR_EVENTS_DELETE = "calendar_events_delete";

    public static final String MSG_GOOGLE_CONNECTION_OK = "google_connection_ok";
    public static final String MSG_GOOGLE_CONNECTION_FAILED = "google_connection_failed";
    public static final String MSG_CALENDARS_LIST_OK = "google_calendar_list_ok";
    public static final String MSG_CALENDARS_LIST_FAILED = "google_calendar_list_failed";
    public static final String MSG_SYNC_OK = "synchronisation_ok";
    public static final String MSG_SYNC_FAILED = "synchronisation_failed";
    public static final String MSG_EVENTS_DELETED = "evenements_supprimes";

    private static final String TIMEZONE_EUROPE_PARIS = "Europe/Paris";

    private static final String APPLICATION_NAME = "RosterToGo";

    private static final String TOKENS_DIRECTORY_PATH = new File(Utils.detectOsAppFolder()).getPath() + File.separatorChar + "tokens";
    ;
    private static final String CREDENTIALS_FILE_PATH = "data/client_secrets.json";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private Calendar service;
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    // link between calendars names and ids
    private HashMap<String, String> mapCalendars;
    private int mode;   // full export, calendars listing, delete events

//    private String calendarId;
    //events to be sent
    private PlanningModel model;
//    private ArrayList<PlanningEvent> planningEvents;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private Date dtBegin, dtEnd;    // for event deletion
    private String calendarsNames;  // for event deletion

    private int n = 0; // increment to th iCalUID

    //TODO: fix bug with batch sending
    public SyncGoogleCalendar() {
        mapCalendars = new HashMap<>();
        sdf.setTimeZone(TimeZone.getTimeZone(TIMEZONE_EUROPE_PARIS));
    }

    public SyncGoogleCalendar(Date begin, Date end, String calendarsNames) {
        this();
        this.dtBegin = begin;
        this.dtEnd = end;
        this.calendarsNames = calendarsNames;

    }

    @Override
    protected String call() throws Exception {
        //first connect to Google
        updateMessage("Synchronisation avec Google Agenda en cours");
        System.out.println("Init Google service...");

        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

        } catch (IOException ex) {
            //return MSG_GOOGLE_CONNECTION_FAILED;
            System.out.println(ex.toString());
            return ex.getMessage();

        }
        System.out.println("Google service created");
        // then get calendar list
        updateMessage("Récupération de la liste des agendas disponibles");
        String msg = fetchCalendarsList();
        if (!msg.equals(MSG_CALENDARS_LIST_OK)) {
            return msg;
        }

        if (mode == MODE_FETCH_CALENDARS) {
            return MSG_CALENDARS_LIST_OK;
        }

        if (mode == MODE_DELETE_EVENTS) {
            String[] cals = calendarsNames.split(";");
            for (String cal : cals) {
                deleteEvents(cal.trim(), dtBegin, dtEnd);
            }
            return MSG_EVENTS_DELETED;
        }
        // send events
        if (mode == MODE_FULL_EXPORT) {
            updateMessage("Suppression des évenements en doublon");

            String[] calendars;
            Set<String> set = new HashSet<>();
            //first delete events from all calendars in all fields
            calendars = MainApp.userPrefs.googleCalendarsFlight.split(";");
            set.addAll(Arrays.asList(calendars));
            calendars = MainApp.userPrefs.googleCalendarsGround.split(";");
            set.addAll(Arrays.asList(calendars));
            calendars = MainApp.userPrefs.googleCalendarsOff.split(";");
            set.addAll(Arrays.asList(calendars));
            calendars = MainApp.userPrefs.googleCalendarsVacation.split(";");
            set.addAll(Arrays.asList(calendars));
            calendars = MainApp.userPrefs.googleCalendarsBlanc.split(";");
            set.addAll(Arrays.asList(calendars));

            for (String calendar : set) {
                deleteOldEvents(calendar.trim());
            }

            calendars = MainApp.userPrefs.googleCalendarsFlight.split(";");
            updateMessage("Envoi des vols vers le(s) calendrier(s)");
            for (String calendar : calendars) {
                sendEvents(SyncGoogleCalendar.CALENDAR_FLIGHT, calendar.trim());
            }

            calendars = MainApp.userPrefs.googleCalendarsGround.split(";");
            updateMessage("Envoi des activités sol vers le(s) calendrier(s)");
            for (String calendar : calendars) {
                sendEvents(SyncGoogleCalendar.CALENDAR_GROUND, calendar.trim());
            }

            calendars = MainApp.userPrefs.googleCalendarsOff.split(";");
            updateMessage("Envoi des jours off vers le(s) calendrier(s)");
            for (String calendar : calendars) {
                sendEvents(SyncGoogleCalendar.CALENDAR_OFF, calendar.trim());
            }

            calendars = MainApp.userPrefs.googleCalendarsVacation.split(";");
            updateMessage("Envoi des congés vers le(s) calendrier(s)");
            for (String calendar : calendars) {
                sendEvents(SyncGoogleCalendar.CALENDAR_VACATION, calendar.trim());
            }

            calendars = MainApp.userPrefs.googleCalendarsBlanc.split(";");
            updateMessage("Envoi des jours blancs vers le(s) calendrier(s)");
            for (String calendar : calendars) {
                sendEvents(SyncGoogleCalendar.CALENDAR_BLANC, calendar.trim());
            }
        }
        return MSG_SYNC_OK;
    }

    private String fetchCalendarsList() {

        // Iterate through entries in calendar list
        String pageToken = null;
        do {
            try {
                CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
                List<CalendarListEntry> items = calendarList.getItems();
                if (items.isEmpty()) {
                    return MSG_CALENDARS_LIST_FAILED;
                }
                items.forEach(calendarListEntry -> {
                    mapCalendars.put(
                            calendarListEntry.getSummary(),
                            calendarListEntry.getId());
                });
                pageToken = calendarList.getNextPageToken();
            } catch (IOException ex) {
                Logger.getLogger(SyncGoogleCalendar.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (pageToken != null);

        return MSG_CALENDARS_LIST_OK;
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private void sendEvents(String eventType, String calendarName) {
        // counter
        int event_ok = 0;
        int event_err = 0;
        // define targeted calendar(s)
        String calendarId;
        if (calendarName == null || "".equals(calendarName)) {
            calendarId = "primary";
        } else {
            calendarId = mapCalendars.get(calendarName);
        }
        if (eventType.equals(SyncGoogleCalendar.CALENDAR_FLIGHT)) {
            if (!MainApp.userPrefs.googleCalendarsFlight.equals("")) {
                for (PlanningEvent pe : model.getAlEvents()) {
                    if (getExportType(pe).equals(CALENDAR_FLIGHT) && pe.isExportable()) {
                        Event event = buildEntry(pe);
                        try {
                            event = service.events().insert(calendarId, event).execute();
                            System.out.println("Events sent successfully");
                            event_ok++;
                        } catch (IOException ex) {
                            System.out.println("Error during events sending");
                            event_err++;
                        }
                    }
                }
            }
        }
        if (eventType.equals(SyncGoogleCalendar.CALENDAR_GROUND)) {
            if (!MainApp.userPrefs.googleCalendarsGround.equals("")) {
                for (PlanningEvent pe : model.getAlEvents()) {
                    if (getExportType(pe).equals(CALENDAR_GROUND) && pe.isExportable()) {
                        Event event = buildEntry(pe);
                        try {
                            event = service.events().insert(calendarId, event).execute();
                            System.out.println("Events sent successfully");
                            event_ok++;
                        } catch (IOException ex) {
                            System.out.println("Error during events sending");
                            event_err++;
                        }
                    }
                }
            }
        }

        if (eventType.equals(SyncGoogleCalendar.CALENDAR_OFF)) {
            if (!MainApp.userPrefs.googleCalendarsOff.equals("")) {
                for (PlanningEvent pe : model.getAlEvents()) {
                    if (getExportType(pe).equals(CALENDAR_OFF) && pe.isExportable()) {
                        Event event = buildEntry(pe);
                        try {
                            event = service.events().insert(calendarId, event).execute();
                            System.out.println("Events sent successfully");
                            event_ok++;
                        } catch (IOException ex) {
                            System.out.println("Error during events sending");
                            event_err++;
                        }
                    }
                }
            }
        }

        if (eventType.equals(SyncGoogleCalendar.CALENDAR_VACATION)) {
            if (!MainApp.userPrefs.googleCalendarsVacation.equals("")) {
                for (PlanningEvent pe : model.getAlEvents()) {
                    if (getExportType(pe).equals(CALENDAR_VACATION) && pe.isExportable()) {
                        Event event = buildEntry(pe);
                        try {
                            event = service.events().insert(calendarId, event).execute();
                            System.out.println("Events sent successfully");
                            event_ok++;
                        } catch (IOException ex) {
                            System.out.println("Error during events sending");
                            event_err++;
                        }
                    }
                }
            }
        }

        if (eventType.equals(SyncGoogleCalendar.CALENDAR_BLANC)) {
            if (!MainApp.userPrefs.googleCalendarsBlanc.equals("")) {
                for (PlanningEvent pe : model.getAlEvents()) {
                    if (getExportType(pe).equals(CALENDAR_BLANC) && pe.isExportable()) {
                        Event event = buildEntry(pe);
                        try {
                            event = service.events().insert(calendarId, event).execute();
                            System.out.println("Events sent successfully");
                            event_ok++;
                        } catch (IOException ex) {
                            System.out.println("Error during events sending");
                            event_err++;
                        }
                    }
                }
            }
        }
        System.out.println(event_ok + " events successfully sent");
        System.out.println(event_err + " errors during sending events");
    }

    private Event buildEntry(PlanningEvent pe) {
        String nl = System.lineSeparator();
        Event event = new Event();
        StringBuilder sb = new StringBuilder();

        // iCalUID
        event.setICalUID(new GregorianCalendar().getTimeInMillis() + "_ROSTERTOGO_" + model.getUserTrigraph());

        // summary
        if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || pe.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
            sb.append(pe.getFltNumber()).append(" ")
                    .append(pe.getIataOrig()).append(" - ")
                    .append(pe.getIataDest());
            if (pe.getLagDest() != PlanningEvent.NO_LAG_AVAIL) {
                sb.append(" (TU ");
                if (pe.getLagDest() < 0) {
                    sb.append(pe.getLagDest()).append(")");
                } else {
                    sb.append("+").append(pe.getLagDest()).append(")");
                }
            }
            event.setSummary(sb.toString());
        } else {
            event.setSummary(pe.getSummary());
        }

        // description
        if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || pe.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
            sb = new StringBuilder();
            if (pe.getAirportOrig() != null) {
                sb.append("Départ:").append(nl);
                sb.append(pe.getAirportOrig().city.toUpperCase()).append(" / ");
                sb.append(pe.getAirportOrig().name).append(nl);
                sb.append(pe.getAirportOrig().country).append(nl).append(nl);
            }
            if (pe.getAirportDest() != null) {
                sb.append("Arrivée:").append(nl);
                sb.append(pe.getAirportDest().city.toUpperCase()).append(" / ");
                sb.append(pe.getAirportDest().name).append(nl);
                sb.append(pe.getAirportDest().country).append(nl).append(nl);
            }

            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT)) {
                sb.append("Fonction : ").append(pe.getFunction());
                sb.append(nl);
                sb.append("Temps de vol : ");
                sb.append(Utils.convertDecimalHourstoHoursMinutes(pe.getBlockTime()));
            } else {
                sb.append("Durée : ");
                sb.append(Utils.convertDecimalHourstoHoursMinutes(pe.getBlockTime()));
            }

            sb.append(nl).append(nl);
            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT)) {

                if (pe.getCrewTec() != null) {
                    sb.append("Pilotes :").append(nl);
                    for (String s : pe.getCrewTec()) {
                        sb.append(s).append(nl);
                    }
                    sb.append(nl);
                }

                if (pe.getCrewCab() != null) {
                    sb.append("PNC :").append(nl);
                    for (String s : pe.getCrewCab()) {
                        sb.append(s).append(nl);
                    }
                    sb.append(nl);
                }
            }
            if (pe.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
                if (pe.getCrewDhd() != null) {
                    sb.append("Mise en place :").append(nl);
                    for (String s : pe.getCrewDhd()) {
                        sb.append(s).append(nl);
                    }
                    sb.append(nl);
                }
            }

            if (!pe.getTraining().equals("")) {
                sb.append(nl).append(nl).append(pe.getTraining());
            }

            if (pe.getCheckInUtc() != null) {
                sb.append(pe.getCheckInUtc()).append(nl);
            }
            if (pe.getCheckInLoc() != null) {
                sb.append(pe.getCheckInLoc()).append(nl);
            }

            if (pe.getsTotalBlock() != null) {
                sb.append(pe.getsTotalBlock()).append(nl);
            }

            if (pe.getsFlightDutyPeriod() != null) {
                sb.append(pe.getsFlightDutyPeriod()).append(nl);
            }
            
            if (pe.getsDutyTime()!= null) {
                sb.append(pe.getsDutyTime()).append(nl);
            }
            
            sb.append(nl);

            if (!pe.getHotelData().equals("")) {
                sb.append(nl).append(nl).append("Hôtel :").append(nl);
                sb.append(pe.getHotelData());
            }

            if (pe.isAcceptedCrewRequest() && MainApp.userPrefs.showAcceptedCrewRequest) {
                sb.append(nl).append("Accpeted Crew Request");
            }
            event.setDescription(sb.toString());
        } else {
            sb.append("Durée : ");
            sb.append(Utils.convertDecimalHourstoHoursMinutes(pe.getBlockTime()));
            if (!pe.getTraining().equals("")) {
                sb.append(nl);
                sb.append(nl);
                sb.append(pe.getTraining());
            }

            if (pe.getCrewGnd() != null) {
                sb.append(nl);
                sb.append(nl);
                sb.append("GND :").append(nl);
                for (String s : pe.getCrewGnd()) {
                    sb.append(s).append(nl);
                }
            }

            if (!pe.getRemark().equals("")) {
                sb.append(nl);
                sb.append(nl);
                sb.append(pe.getRemark());
            }

            if (!pe.getDescription().equals("")) {
                sb.append(nl);
                sb.append(nl);
                sb.append(pe.getDescription());
            }
            event.setDescription(sb.toString());
        }

        // date and time 
        // create temp gregoriancalendars to store modified date
        GregorianCalendar calBegin = new GregorianCalendar();
        calBegin.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        calBegin.setTime(pe.getGcBegin().getTime());

        GregorianCalendar calEnd = new GregorianCalendar();
        calEnd.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        calEnd.setTime(pe.getGcEnd().getTime());

        if (pe.isDayEvent()) {
            String dateBegin = sdf.format(calBegin.getTime());
            calEnd.add(java.util.Calendar.DATE, 1);
            String dateEnd = sdf.format(calEnd.getTime());

            DateTime begin = new DateTime(dateBegin);
            event.setStart(new EventDateTime().setDate(begin));
            DateTime end = new DateTime(dateEnd);
            event.setEnd(new EventDateTime().setDate(end));

        } else {
            DateTime start = new DateTime(pe.getGcBegin().getTimeInMillis());
            event.setStart(new EventDateTime().setDateTime(start));
            DateTime end = new DateTime(pe.getGcEnd().getTimeInMillis());
            event.setEnd(new EventDateTime().setDateTime(end));
        }

        // set an ExtendedProperty to tag RosterToGo event for future deletion
        Event.ExtendedProperties extProperties = new Event.ExtendedProperties();
        Map<String, String> properties = new HashMap<>();
        properties.put("id", "rostertogo");
        extProperties.setShared(properties);
        event.setExtendedProperties(extProperties);

        if (MainApp.userPrefs.googleColorizeEvents) {
            event.setColorId(defineColorId(pe));
        }
        return event;
    }

    private void deleteOldEvents(String calendarName) {
        // create new instances to prevent modifying original values
        GregorianCalendar calBegin = new GregorianCalendar();
        if (!model.modeOnline) {
            calBegin.setTime(model.getAlEvents().get(0).getGcBegin().getTime());
        }
        calBegin.set(GregorianCalendar.HOUR_OF_DAY, 0);
        calBegin.set(GregorianCalendar.MINUTE, 0);
        DateTime dtDeleteBegin = new DateTime(calBegin.getTime());

        GregorianCalendar calEnd = new GregorianCalendar();
        calEnd.setTime(model.getAlEvents().get(model.getAlEvents().size() - 1).getGcEnd().getTime());
        calEnd.add(GregorianCalendar.DAY_OF_MONTH, 1); // add one day to get midnight of the last day
        calEnd.set(GregorianCalendar.HOUR_OF_DAY, 0);
        calEnd.set(GregorianCalendar.MINUTE, 0);
        DateTime dtDeleteEnd = new DateTime(calEnd.getTime());

        // define the ExtendedPropoerty to filter RosterToGo events
        String property = "id=rostertogo";
        ArrayList<String> extProperties = new ArrayList<>();
        extProperties.add(property);

        String calendarId;
        if (calendarName == null || "".equals(calendarName)) {
            calendarId = "primary";
        } else {
            calendarId = mapCalendars.get(calendarName);
        }

        // Iterate over the events in the specified calendar
        Events evnts;
        String pageToken = null;

        try {
            do {
                evnts = service.events().list(calendarId)
                        .setPageToken(pageToken)
                        .setSharedExtendedProperty(extProperties)
                        .setTimeMin(dtDeleteBegin).setTimeMax(dtDeleteEnd).execute();

                if (!evnts.getItems().isEmpty()) {

                    JsonBatchCallback<Void> callback = new JsonBatchCallback<Void>() {
                        @Override
                        public void onSuccess(Void v, HttpHeaders responseHeaders) {
                            System.out.println("élément effacé");
                        }

                        @Override
                        public void onFailure(GoogleJsonError e,
                                HttpHeaders responseHeaders) {
                            System.out.println("Error Message: " + e.getMessage());
                        }
                    };

                    BatchRequest batch = service.batch();
                    for (Event event : evnts.getItems()) {
                        service.events().delete(calendarId, event.getId()).queue(batch, callback);
                    }

                    if (batch.size() != 0) {
                        batch.execute();
                    }
                }
                pageToken = evnts.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException ex) {
        }
    }

    private void deleteEvents(String calendarName, Date begin, Date end) {

        DateTime dtDeleteBegin = new DateTime(begin.getTime());
        DateTime dtDeleteEnd = new DateTime(end.getTime());

        // define the ExtendedPropoerty to filter RosterToGo events
        String property = "id=rostertogo";
        ArrayList<String> extProperties = new ArrayList<>();
        extProperties.add(property);

        String calendarId;
        if (calendarName == null || "".equals(calendarName)) {
            calendarId = "primary";
        } else {
            calendarId = mapCalendars.get(calendarName);
        }

        // Iterate over the events in the specified calendar
        String pageToken = null;
        Events evnts;
        try {
            do {
                evnts = service.events().list(calendarId)
                        .setPageToken(pageToken)
                        .setSharedExtendedProperty(extProperties)
                        .setTimeMin(dtDeleteBegin).setTimeMax(dtDeleteEnd).execute();

                if (!evnts.getItems().isEmpty()) {

                    JsonBatchCallback<Void> callback = new JsonBatchCallback<Void>() {
                        @Override
                        public void onSuccess(Void v, HttpHeaders responseHeaders) {
                            System.out.println("élément effacé");
                        }

                        @Override
                        public void onFailure(GoogleJsonError e,
                                HttpHeaders responseHeaders) {
                            System.out.println("Error Message: " + e.getMessage());
                        }
                    };

                    BatchRequest batch = service.batch();
                    for (Event event : evnts.getItems()) {
                        service.events().delete(calendarId, event.getId()).queue(batch, callback);
                    }

                    if (batch.size() != 0) {
                        batch.execute();
                    }
                }
                pageToken = evnts.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException ex) {
        }
    }

    public ArrayList<String> getCalendarNames() {
        ArrayList list = new ArrayList<>();
        mapCalendars.keySet().stream().forEach((key) -> {
            list.add(key);
        });
        return list;
    }

//    public void setEvents(ArrayList<PlanningEvent> events) {
//        this.planningEvents = events;
//    }
    public void setModel(PlanningModel model) {
        this.model = model;
    }

    private String getExportType(PlanningEvent event) {
        switch (event.getCategory()) {
            case PlanningEvent.CAT_FLIGHT:
                return CALENDAR_FLIGHT;
            case PlanningEvent.CAT_DEAD_HEAD:
                return CALENDAR_FLIGHT;
            case PlanningEvent.CAT_CHECK_IN:
                return CALENDAR_FLIGHT;
            case PlanningEvent.CAT_HOTEL:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SYND:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SYND_CSE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_UNION:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_ILLNESS:
                return CALENDAR_OFF;
            case PlanningEvent.CAT_FATIGUE:
                return CALENDAR_OFF;
            case PlanningEvent.CAT_ENFANT_MALADE:
                return CALENDAR_OFF;
            case PlanningEvent.CAT_MEDICAL:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_OFF:
                return CALENDAR_OFF;
            case PlanningEvent.CAT_OFF_DDA:
                return CALENDAR_OFF;
            case PlanningEvent.CAT_OFF_RECUP:
                return CALENDAR_OFF;
            case PlanningEvent.CAT_REPOS_POST_COURRIER:
                return CALENDAR_OFF;
            case PlanningEvent.CAT_TYPE_RATING:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIMU:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_APRS:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIM_C1:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIM_C2:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIM_E1:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIM_E2:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIM_LOE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIM_ENT1:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIM_UPRT:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SIM_LVO:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_BUR_MIN:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_CRM_COURSE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_E_LEARNING:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_E_LEARNING_TECH_LOG:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_GROUND_COURSE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_MDC_COURSE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SAFETY_COURSE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_SECURITY_COURSE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_DANGEROUSGOODS_COURSE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_ENTRETIEN_PRO:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_REUNION_COMPAGNIE:
                return CALENDAR_GROUND;
            case PlanningEvent.CAT_VACATION:
                return CALENDAR_VACATION;
            case PlanningEvent.CAT_VACATION_OFF_CAMPAIGN:
                return CALENDAR_VACATION;
            case PlanningEvent.CAT_VACATION_BIRTH:
                return CALENDAR_VACATION;
            case PlanningEvent.CAT_CONGES_SUR_BLANC:
                return CALENDAR_VACATION;
            case PlanningEvent.CAT_BLANC:
                return CALENDAR_BLANC;
            case PlanningEvent.CAT_ABSENCE_A_JUSTIFIER:
                return CALENDAR_OFF;
            default:
                return CALENDAR_FLIGHT;
        }
    }

    private String defineColorId(PlanningEvent pe) {
        switch (pe.getCategory()) {
            case PlanningEvent.CAT_FLIGHT:
                return "9";
            case PlanningEvent.CAT_DEAD_HEAD:
                return "1";
            case PlanningEvent.CAT_CHECK_IN:
                return "1";
            case PlanningEvent.CAT_HOTEL:
                return "0";
            case PlanningEvent.CAT_SYND:
                return "6";
            case PlanningEvent.CAT_UNION:
                return "6";
            case PlanningEvent.CAT_ILLNESS:
                return "5";
            case PlanningEvent.CAT_FATIGUE:
                return "5";
            case PlanningEvent.CAT_ENFANT_MALADE:
                return "5";
            case PlanningEvent.CAT_ACCIDENT_TRAVAIL:
                return "5";
            case PlanningEvent.CAT_OFF_MALADIE:
                return "5";
            case PlanningEvent.CAT_MEDICAL:
                return "11";
            case PlanningEvent.CAT_OFF:
                return "10";
            case PlanningEvent.CAT_OFF_DDA:
                return "10";
            case PlanningEvent.CAT_OFF_GARANTI:
                return "10";
            case PlanningEvent.CAT_OFF_RECUP:
                return "10";
            case PlanningEvent.CAT_REPOS_POST_COURRIER:
                return "10";
            case PlanningEvent.CAT_JOUR_INACTIVITE_SPECIAL_ACTIVITE_PARTIELLE:
                return "10";
            case PlanningEvent.CAT_TYPE_RATING:
                return "4";
            case PlanningEvent.CAT_SIMU:
                return "4";
            case PlanningEvent.CAT_SIM_C1:
                return "4";
            case PlanningEvent.CAT_SIM_C2:
                return "4";
            case PlanningEvent.CAT_SIM_E1:
                return "4";
            case PlanningEvent.CAT_SIM_E2:
                return "4";
            case PlanningEvent.CAT_SIM_FT1:
                return "4";
            case PlanningEvent.CAT_SIM_FT2:
                return "4";
            case PlanningEvent.CAT_SIM_LOE:
                return "4";
            case PlanningEvent.CAT_SIM_LVO:
                return "4";
            case PlanningEvent.CAT_SIM_UPRT:
                return "4";
            case PlanningEvent.CAT_SIM_ENT1:
                return "4";
            case PlanningEvent.CAT_BUR_MIN:
                return "4";
            case PlanningEvent.CAT_BUREAU_PNT:
                return "4";
            case PlanningEvent.CAT_ENTRETIEN_PRO:
                return "4";
            case PlanningEvent.CAT_REUNION_COMPAGNIE:
                return "4";
            case PlanningEvent.CAT_REUNION_INSTRUCTEURS:
                return "4";
            case PlanningEvent.CAT_VACATION:
                return "10";
            case PlanningEvent.CAT_VACATION_PRINCIPAL_PERIOD:
                return "10";
            case PlanningEvent.CAT_VACATION_OFF_CAMPAIGN:
                return "10";
            case PlanningEvent.CAT_VACATION_BIRTH:
                return "10";
            case PlanningEvent.CAT_CONGES_SUR_BLANC:
                return "10";
            case PlanningEvent.CAT_BLANC:
                return "8";
            case PlanningEvent.CAT_ABSENCE_A_JUSTIFIER:
                return "6";

            default:
                return "4";
        }
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}
