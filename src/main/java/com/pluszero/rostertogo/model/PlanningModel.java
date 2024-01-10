/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.model;

import com.pluszero.rostertogo.ActivityAnalyser;
import com.pluszero.rostertogo.DateComparator;
import com.pluszero.rostertogo.MainApp;
import com.pluszero.rostertogo.PdfManager;
import com.pluszero.rostertogo.Utils;
import java.io.File;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.TimeZone;

/**
 *
 * @author Cyril
 */
public class PlanningModel {

    private final static int ONE_HOUR = 3600000;
    private final static int ONE_DAY = 86400000;

    private static final String JOUR_BLANC = "BLANC";
    private static final String TIMEZONE_PARIS = "Europe/Paris";

    public boolean modeOnline = true;
    private ArrayList<PlanningEvent> alEvents;
    private ArrayList<String> airports;
    private ActivityAnalyser activityAnalyser, activityAnalyserAF;

    private String userTrigraph = "XYZ";
    private HashSet<String> unknownCrew;

    private SimpleDateFormat sdfDate;
    private final String[] shortMonths = new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
    private final DateFormatSymbols dfs = new DateFormatSymbols();

    public PlanningModel(ArrayList<String> airports) {
        alEvents = new ArrayList<>();
        this.airports = airports;

        // for the unknown crew list format
        dfs.setShortMonths(shortMonths);
        sdfDate = new SimpleDateFormat("ddMMM", dfs);
    }

    public ArrayList<PlanningEvent> getAlEvents() {
        return alEvents;
    }

    /*
    Deal with splitted "jour off" or "malade" or "Congés" (2300Z->2359Z & 0000Z->2259Z) by removing 
    any event which has "jour off" in summary and whose duration is < 2h
     */
    public void fixSplittedActivities() {
        HashSet<PlanningEvent> hashSet = new HashSet<>();

        for (PlanningEvent pe : alEvents) {
            if (pe.isDayEvent()) {
                long diff = pe.getGcEnd().getTimeInMillis() - pe.getGcBegin().getTimeInMillis();
                if (diff > 0 && diff <= ONE_HOUR * 2) {
                    continue;
                }
                //set same begin and end time for all day events
                pe.getGcBegin().set(Calendar.HOUR_OF_DAY, 10);
                pe.getGcBegin().set(Calendar.MINUTE, 0);
                pe.getGcBegin().set(Calendar.SECOND, 0);
                pe.getGcBegin().set(Calendar.MILLISECOND, 0);

                pe.getGcEnd().set(Calendar.HOUR_OF_DAY, 10);
                pe.getGcEnd().set(Calendar.MINUTE, 0);
                pe.getGcEnd().set(Calendar.SECOND, 0);
                pe.getGcEnd().set(Calendar.MILLISECOND, 0);
            }
            hashSet.add(pe);
        }
        // ensure no duplicate and sorted by date
        alEvents = new ArrayList<>(hashSet);
        Collections.sort(alEvents, new DateComparator());
    }

    public void addJoursBlanc() {
        GregorianCalendar cal = new GregorianCalendar(); // init at today, 12h00 loc

        if (!modeOnline) {
            cal.setTimeInMillis(alEvents.get(alEvents.size() - 1).getGcBegin().getTimeInMillis());
        }
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        cal.setTimeZone(TimeZone.getTimeZone(TIMEZONE_PARIS));

        // planning is set each friday for 4 weeks + 3 days
        int span = findMaxPlanningDays(cal);

        for (int i = 0; i < span; i++) {
            boolean flagJourBlanc = true;
            for (PlanningEvent pe : alEvents) {
                GregorianCalendar gce = new GregorianCalendar();
                gce.setTimeZone(TimeZone.getTimeZone(TIMEZONE_PARIS));
                gce.setTime(pe.getGcBegin().getTime());
                // test on DAY_OF_YEAR to avoid error from one month to another
                if (gce.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)) {
                    flagJourBlanc = false;
                    break;
                }
            }
            if (flagJourBlanc) {
                PlanningEvent event = new PlanningEvent();
                //set same begin and end time for all day events
                event.setGcBegin((GregorianCalendar) cal.clone());
                event.setGcEnd((GregorianCalendar) cal.clone());
                event.setLabel(JOUR_BLANC);
                event.setCategory(PlanningEvent.CAT_BLANC);
                alEvents.add(event);
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        Collections.sort(alEvents, new DateComparator());
    }

    /*
    determine number of days of visiblity, knowing that the planning is set 
    every friday for the upcoming 4 weeks + 3 days
     */
    public int findMaxPlanningDays(GregorianCalendar cal) {
        // define the planning span which is either the farest event or a number
        // of days based on the day of the week

        // compute number of days between last and first event
        long last = alEvents.get(alEvents.size() - 1).getGcBegin().getTimeInMillis();
        long first = alEvents.get(0).getGcBegin().getTimeInMillis();
        int maxActual = (int) ((last - first) / ONE_DAY);

        int maxPlanned;
        switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.FRIDAY:
                maxPlanned = 32;
                break;
            case Calendar.SATURDAY:
                maxPlanned = 31;
                break;
            case Calendar.SUNDAY:
                maxPlanned = 30;
                break;
            case Calendar.MONDAY:
                maxPlanned = 29;
                break;
            case Calendar.TUESDAY:
                maxPlanned = 28;
                break;
            case Calendar.WEDNESDAY:
                maxPlanned = 27;
                break;
            case Calendar.THURSDAY:
                maxPlanned = 26;
                break;
            default:
                maxPlanned = 32;
                break;
        }

        if (maxActual > maxPlanned) {
            return maxActual;
        } else {
            return maxPlanned;
        }
    }

    public void addDataFromPDF(File file, String pdfContent) throws Exception {

        PdfManager manager = new PdfManager();

        // deal with pdf according to mode (online/offline)
        if (file != null) {
            String path = file.getParent();
            String filename = file.getName();
            File pdfFile = new File(path + File.separator + filename.replace(".ics", ".pdf"));
            String content = Utils.readFile(pdfFile).toString();
            manager.extractPdfData(content);
            //MainApp.userPrefs.crewFunction = manager.extractCrewFunction(content);
        } else if (pdfContent != null) {
            manager.extractPdfData(pdfContent);
            //MainApp.userPrefs.crewFunction = manager.extractCrewFunction(pdfContent);
        }

        for (PlanningEvent pe : alEvents) {
            // first find the event in the pdf. If null, do not update the fields
            // (case of multiple files load)
            String pdfEvent = manager.findPdfEvent(pe.getGcBegin());
            if (pdfEvent == null) {
                continue;
            }
            // crew
            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT)) {
                pe.setCrewList(manager.extractCrewList(pdfEvent));
                if (pe.getCrewList() != null) {
                    pe.setCrewTec(manager.extractCrewType(pe.getCrewList(), PdfManager.TYPE_PILOT));
                    pe.setCrewCab(manager.extractCrewType(pe.getCrewList(), PdfManager.TYPE_CABIN));
                }
            }
            // deadheading crew
            if (pe.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
                pe.setCrewList(manager.extractCrewList(pdfEvent));
                pe.setCrewDhd(manager.extractCrewType(pe.getCrewList(), PdfManager.TYPE_DEADHEAD));
            }

            // hotel data
            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || pe.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
                String data = manager.findHotelDetails(pe.getGcBegin());
                if (data != null) {
                    pe.setHotelData(data);
                }
            }

            // flight remarks
//            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT)) {
//                String data = manager.findRemarks(
//                        pe.getGcBegin(),
//                        pe.getIataOrig(),
//                        pe.getIataDest());
//                if (data != null) {
//                    pe.setRemark(data);
//                }
//            }
            // Additional time data
            pe.setCheckInUtc(manager.extractAdditionalTimeData(pdfEvent, PdfManager.CHECK_IN_UTC));
            pe.setCheckInLoc(manager.extractAdditionalTimeData(pdfEvent, PdfManager.CHECK_IN_LOC));
            pe.setBlkFdpDuty(manager.extractAdditionalTimeData(pdfEvent, PdfManager.BLK_FDP_DUTY));
            pe.setsTotalBlock(manager.extractAdditionalTimeData(pdfEvent, PdfManager.TOTAL_BLK));
            pe.setsFlightDutyPeriod(manager.extractAdditionalTimeData(pdfEvent, PdfManager.FDP));
            pe.setsDutyTime(manager.extractAdditionalTimeData(pdfEvent, PdfManager.DUTY));

            if (pe.isSimActivity()) {
                pe.setCrewList(manager.extractCrewList(pdfEvent));
                pe.setCrewGnd(manager.extractCrewType(pe.getCrewList(), PdfManager.TYPE_GROUND));
                String data = manager.findTraining(pe.getGcBegin());
                if (data != null) {
                    pe.setTraining(data);
                }

                data = manager.findRemarks(pe.getGcBegin());
                if (data != null) {
                    pe.setRemark(data);
                }
            }
            cleanAcceptedCrewRequest(pe);
        }

        System.out.println("PDF data added");
    }

    /**
     * check for crew requested events, remove multiple occurrence and set event
     * boolean
     */
    private void cleanAcceptedCrewRequest(PlanningEvent pe) {
        if (pe.getRemark().contains("Accepted Crew Request")) {
            pe.setAcceptedCrewRequest(true);
            String s = pe.getRemark().replace("Accepted Crew Request", "");
            pe.setRemark(s);
        }

    }

    /**
     * scan the arrayList and detect all events that are first of the day
     */
    public void factorizeDays() {
        PlanningEvent previous = null;
        for (PlanningEvent pe : alEvents) {
            // for the first item in list
            if (previous == null) {
                previous = pe;
                pe.setFirstEventOfDay(true);
            } else if (previous.getGcBegin().get(Calendar.DAY_OF_MONTH) != pe.getGcEnd().get(Calendar.DAY_OF_MONTH)) {
                pe.setFirstEventOfDay(true);
                previous = pe;
            }
        }
    }

    /**
     * copy crew from first leg to the other legs and search for unknown crew
     * (trigraph not yet in database)
     */
    public void copyCrew() {
        ArrayList<CrewMember> crewMembers = null;

        for (PlanningEvent pe : alEvents) {
            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT)) {
                if (!pe.getAlCrewMembers().isEmpty()) {
                    crewMembers = pe.getAlCrewMembers();

                } else {
                    pe.setCrewMembers(crewMembers);
                }
            }
        }
    }

    public void findAirportDetails() {
        for (PlanningEvent pe : alEvents) {
            if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                for (String s : airports) {
                    // remove quote if any
                    s = s.replace("\"", "");
                    String[] result = s.split(",");
                    if (result.length > 5 && result[4].equals(pe.getIataOrig())) {
                        pe.setAirportOrig(new Airport(result[4], result[1], result[5], result[2], result[3]));
                    }
                    if (result.length > 5 && result[4].equals(pe.getIataDest())) {
                        pe.setAirportDest(new Airport(result[4], result[1], result[5], result[2], result[3]));
                    }

                    if (pe.getAirportOrig() != null && pe.getAirportDest() != null) {
                        break;
                    }
                }
            }
        }
    }

    public void replaceUnknownWithFoundCrew(HashSet<String> foundCrew) {
        for (PlanningEvent event : alEvents) {
            if (!event.getCategory().equals(PlanningEvent.CAT_FLIGHT)) {
                continue;
            }
            for (CrewMember crewMember : event.getAlCrewMembers()) {
                if (crewMember.surname.equals(CrewMember.UNKNOWN_TRIGRAPH)) {
                    for (String string : foundCrew) {
                        if (string.startsWith(crewMember.trigraph)) {
                            crewMember.surname = string.substring(4);
                        }
                    }
                }
            }
        }
    }

    /**
     * determine the first and last dates of planning (dd/MM/yyyy)
     *
     * @return a string containing first and last days
     */
    public String getPlanningFirstAndLastDatesAsString() {
        Date d1 = alEvents.get(0).getGcBegin().getTime();
        Date d2 = alEvents.get(alEvents.size() - 1).getGcEnd().getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(d1) + " au " + sdf.format(d2);
    }

    public void computeActivityFigures() {
        activityAnalyser = new ActivityAnalyser(
                this,
                MainApp.userPrefs.crewFunction,
                ActivityAnalyser.PAY_MODE_TO);
        activityAnalyserAF = new ActivityAnalyser(
                this,
                MainApp.userPrefs.crewFunction,
                ActivityAnalyser.PAY_MODE_AF);
    }

    public void sortByAscendingDate() {
        Collections.sort(alEvents, new DateComparator());
    }

    public ActivityAnalyser getActivityAnalyser() {
        return activityAnalyser;
    }

    public ActivityAnalyser getActivityAnalyserAF() {
        return activityAnalyserAF;
    }

    public String getUserTrigraph() {
        return userTrigraph;
    }

    public void setUserTrigraph(String userTrigraph) {
        this.userTrigraph = userTrigraph;
    }
}
