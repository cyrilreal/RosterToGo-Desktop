package com.pluszero.rostertogo.model;

import com.pluszero.rostertogo.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Cyril
 */
public class PlanningEvent {

    public static final long ONE_DAY_IN_MILLISECONDS = 86400000;
    public static final int NO_LAG_AVAIL = 99;
    public static final float COEFF_HCNUIT = 0.2f;

    public final static String CAT_FLIGHT = "FLT";
    public final static String CAT_DEAD_HEAD = "DHD";
    public final static String CAT_CHECK_IN = "ENGS";
    public final static String CAT_HOTEL = "HOT";
    public final static String CAT_UNION = "JDD";
    public final static String CAT_SYND = "JDDC";
    public final static String CAT_SYND_CSE = "RCSE";
    public final static String CAT_TYPE_RATING = "QT";
    public final static String CAT_SIMU = "SIMU";
    public final static String CAT_SIM_E2 = "E2";
    public final static String CAT_SIM_LOE1 = "LOE1";
    public final static String CAT_SIM_LOE2 = "LOE2";
    public final static String CAT_SIM_FT1 = "FT1";
    public final static String CAT_SIM_FT2 = "FT2";
    public final static String CAT_SIM_E1 = "E1";
    public final static String CAT_SIM_C1 = "C1";
    public final static String CAT_SIM_C2 = "C2";
    public final static String CAT_SIM_LOE = "LOE";
    public final static String CAT_SIM_UPRT = "UPRT";
    public final static String CAT_SIM_ENT1 = "ENT1";
    public final static String CAT_SIM_LVO = "LVO";
    public final static String CAT_BUR_MIN = "BUR_";
    public final static String CAT_E_LEARNING = "E_LE";
    public final static String CAT_E_LEARNING_TECH_LOG = "E_ET";
    public final static String CAT_GROUND_COURSE = "CS_M";
    public final static String CAT_MDC_COURSE = "MDC_";
    public final static String CAT_MDC_INSTRUCTOR = "CS_C";
    public final static String CAT_SECURITY_COURSE = "SUR_";
    public final static String CAT_DANGEROUSGOODS_COURSE = "MD_E";
    public final static String CAT_CRM_COURSE = "CRMT";
    public final static String CAT_APRS = "APRS";
    public final static String CAT_E_APRS = "E_AP";
    public final static String CAT_SAFETY_COURSE = "SS1_";
    public final static String CAT_MEDICAL = "VM";
    public final static String CAT_OFF_DDA = "OFFD";
    public final static String CAT_OFF_GARANTI = "OFFG";
    public final static String CAT_OFF = "OFF";
    public final static String CAT_OFF_RECUP = "OFFR";
    public final static String CAT_REPOS_POST_COURRIER = "RPC";
    public final static String CAT_JOUR_INACTIVITE_SPECIAL_ACTIVITE_PARTIELLE = "JISA";
    public final static String CAT_ILLNESS = "HS";
    public final static String CAT_FATIGUE = "UNFI";
    public final static String CAT_OFF_MALADIE = "OFFH";
    public final static String CAT_ENFANT_MALADE = "EMAL";
    public final static String CAT_ACCIDENT_TRAVAIL = "AT";
    public final static String CAT_VACATION = "CA";
    public final static String CAT_VACATION_PRINCIPAL_PERIOD = "CAP";
    public final static String CAT_VACATION_OFF_CAMPAIGN = "CAHC";
    public final static String CAT_CONGES_SUR_BLANC = "CPBL";
    public final static String CAT_VACATION_BIRTH = "CEX";
    public final static String CAT_GREV = "GREV";
    public final static String CAT_NON_PLANNIFIABLE = "NPL";
    public final static String CAT_ENTRETIEN_PRO = "ENTP";
    public final static String CAT_REUNION_COMPAGNIE = "MEET";
    public final static String CAT_REUNION_INSTRUCTEURS = "MINT";
    public final static String CAT_REUNION_CONCERTATION = "WORK";
    public final static String CAT_BUREAU_PNT = "BURT";
    public final static String CAT_ABSENCE_A_JUSTIFIER = "ABST";
    public final static String CAT_SWAP_BLANC_VOL = "BLAN"; //swap vol contre blanc
    public final static String CAT_BLANC = "BLANC"; // not a Transavia code
    public final static String[] FLIGHT_ACTIVITIES = {"FLT", "DHD", "VOL"}; // "VOL" for ToSync compatibility
    public final static String[] PAID_GROUND_ACTIVITIES = {"SIMU", "APRS", "E1", "E2", "C1", "C2", "LOE", "LOE1", "LOE2", "FT1", "FT2", "E_LE", "E_ET", "CS_M", "MDC_", "SUR_", "MD_E", "CRMT", "SS1_", "VMT", "VM", "ENTP", "MEET", "WORK", "JDD", "JDDC", "RCSE", "BURT"};
    public final static String[] PAID_SIM_ACTIVITY = {"SIMU", "E1", "E2", "C1", "C2", "LOE", "LOE1", "LOE2", "FT1", "FT2", "LVO", "ENT1", "UPRT"};
    public final static String[] PAID_ILLNESS = {"OFFH", "HS"};
    public final static String[] PAID_VACATION = {"CA", "CAP", "CAHC", "CEX", "CPBL"};
    public final static String[] PAID_GROUND_ACTIVITIES_INSTRUCTOR = {"CS_C", "BUR_", "MINT"};
    public final static String[] OFF_OR_VACATION = {"OFF", "OFFG", "OFFD", "OFFR", "REPOS", "CA", "CAHC", "CAP"}; //REPOS is for ToSync compatibility

    private GregorianCalendar gcBegin;
    private GregorianCalendar gcEnd;
    private String summary = "";
    private String category = "";
    private String description = "";
    private String iataOrig = "";
    private Airport airportOrig;
    private String iataDest = "";
    private Airport airportDest;
    private String fltNumber = "";
    private String[] crewCab;
    private String[] crewTec;
    private String[] crewDhd;
    private String[] crewGnd;
    private String[] crewList;
    private ArrayList<CrewMember> crew;
    private String training = "";
    private String remark = "";
    private String hotelData = "";
    private String function = "";
    private String checkInUtc = "";
    private String checkInLoc = "";
    private String blkFdpDuty = "";
    private String sTotalBlock = "";
    private String sFlightDutyPeriod = "";
    private String sDutyTime = "";
    private int lagDest = 0;
    private float blockTime = 0;  // in decimal hours
    private long fdpTime = 0; // flight duty period containing the flight, in milliseconds
    private float hv = 0;    // in decimal jours, and for flights only
    private float hv_r = 0;    // in decimal jours, and for flights only
    private float hvNuit = 0; // in decimal hours, and for flights only
    private float hcNuit = 0; // in decimal hours, and for flights only
    private boolean firstEventOfDay;
    private boolean acceptedCrewRequest = false;
    private boolean exportable = true;

    // for the TableView
    private SimpleStringProperty dateString;
    private SimpleStringProperty activityString;
    private ObjectProperty<GregorianCalendar> dateProperty;

    // credit hours part of the tooltip (to avoid passing full planning to eventCell)
    private String tooltipHcContent;

    public PlanningEvent() {
    }

    public PlanningEvent(GregorianCalendar dateStart, GregorianCalendar dateEnd, String summary, String category, String description) {
        this.gcBegin = dateStart;
        this.gcEnd = dateEnd;
        this.summary = summary.trim();
        this.category = category.trim();
        this.description = description.trim();

        if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(category::equals)) {
            parseSummary(summary);
            parseDescription();
        }

        blockTime = Utils.convertMillisecondsToDecimalHours(
                dateEnd.getTimeInMillis() - dateStart.getTimeInMillis());

        computeHCNuit();
        dateString = new SimpleStringProperty(dateStart.toString());
        dateProperty = new SimpleObjectProperty<>(gcBegin);
        activityString = new SimpleStringProperty(summary);
    }

    public GregorianCalendar getGcEnd() {
        return gcEnd;
    }

    public GregorianCalendar getGcBegin() {
        return gcBegin;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setGcEnd(GregorianCalendar gcEnd) {
        this.gcEnd = gcEnd;
    }

    public void setGcBegin(GregorianCalendar gcBegin) {
        this.gcBegin = gcBegin;
    }

    public void setLabel(String label) {
        this.summary = label;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFltNumber() {
        return fltNumber;
    }

    public void setFltNumber(String fltNumber) {
        this.fltNumber = fltNumber;
    }

    public Airport getAirportOrig() {
        return airportOrig;
    }

    public void setAirportOrig(Airport airportOrig) {
        this.airportOrig = airportOrig;
    }

    public Airport getAirportDest() {
        return airportDest;
    }

    public void setAirportDest(Airport airportDest) {
        this.airportDest = airportDest;
    }

    public String getIataDest() {
        return iataDest;
    }

    public void setIataOrig(String iataOrig) {
        this.iataOrig = iataOrig;
    }

    public void setIataDest(String iataDest) {
        this.iataDest = iataDest;
    }

    public void setLagDest(int lagDest) {
        this.lagDest = lagDest;
    }

    public String getIataOrig() {
        return iataOrig;
    }

    public int getLagDest() {
        return lagDest;
    }

    private void parseSummary(String summary) {
        String target;
        Pattern pattern;
        Matcher result;
        // search flight number (deal with case of no flight number in deadheading)
        if (summary.contains(" ")) {
            fltNumber = summary.substring(0, summary.indexOf(" "));
        } else {
            fltNumber = "NIL";
        }

        // origin and destination
        target = "([a-zA-Z]{3})[-\\*]([a-zA-Z]{3})";
        pattern = Pattern.compile(target);
        result = pattern.matcher(summary);

        if (result.find()) {
            iataOrig = result.group(1);
            iataDest = result.group(2);
        } else {
            // ToSync compatibility
            target = "([A-Z]{3}) [-\\*] ([A-Z]{3})";
            pattern = Pattern.compile(target);
            result = pattern.matcher(summary);
            if (result.find()) {
                iataOrig = result.group(1);
                iataDest = result.group(2);
            }
        }

        // time shift at destination
        target = Pattern.quote("(") + "\\W\\d{4}" + Pattern.quote(")");
        pattern = Pattern.compile(target);
        result = pattern.matcher(summary);
        while (result.find()) {
            String lag = result.group(0).substring(1, 4);
            lagDest = Integer.parseInt(lag);
            return;
        }
        lagDest = NO_LAG_AVAIL;   // set lag to 99 if no lag detected ( case of deadheading )
    }

    private void parseDescription() {
        String[] desc = description.split("\n");
        // get function
        for (String s : desc) {
            if (s.startsWith("FCT")) {
                function = s.substring(s.indexOf(":") + 1).trim();
                break;
            }
        }

        // get crew
        crew = new ArrayList<>();
        if (description.contains("Crew Member")) {
            String crewTrigraph = "[A-Z]{3}";
            String crewChain;
            Matcher result;
            Pattern regex;
            // Get pilots
            crewChain = "((\\W?T:\\s?)([A-Z]{3}-?)++)";
            for (String s : desc) {
                regex = Pattern.compile(crewChain);
                result = regex.matcher(s);
                if (result.find()) {
                    regex = Pattern.compile(crewTrigraph);
                    result = regex.matcher(s);
                    while (result.find()) {
                        crew.add(new CrewMember(
                                result.group(0),
                                "",
                                CrewMember.UNKNOWN_TRIGRAPH,
                                CrewMember.UNKNOWN_PILOT,
                                ""));
                    }
                    break;
                }
            }

            // Get cabin crew
            crewChain = "((\\W?C:\\s?)([A-Z]{3}-?)++)";
            regex = Pattern.compile(crewChain);
            for (String s : desc) {
                result = regex.matcher(s);
                if (result.find()) {
                    regex = Pattern.compile(crewTrigraph);
                    result = regex.matcher(s);
                    while (result.find()) {
                        crew.add(new CrewMember(
                                result.group(0),
                                "",
                                CrewMember.UNKNOWN_TRIGRAPH,
                                CrewMember.UNKNOWN_FLIGHT_ATTENDANT,
                                ""));
                    }
                    break;
                }
            }
        }
    }

    public boolean isDayEvent() {
        switch (category) {
            case CAT_OFF:
                return true;

            case CAT_OFF_RECUP:
                return true;

            case CAT_OFF_MALADIE:
                return true;

            case CAT_OFF_DDA:
                return true;

            case CAT_OFF_GARANTI:
                return true;

            case CAT_REPOS_POST_COURRIER:
                return true;

            case CAT_JOUR_INACTIVITE_SPECIAL_ACTIVITE_PARTIELLE:
                return true;

            case CAT_VACATION:
                return true;

            case CAT_VACATION_PRINCIPAL_PERIOD:
                return true;

            case CAT_VACATION_OFF_CAMPAIGN:
                return true;

            case CAT_VACATION_BIRTH:
                return true;

            case CAT_CONGES_SUR_BLANC:
                return true;

            case CAT_BLANC:
                return true;

            case CAT_SWAP_BLANC_VOL:
                return true;

            case CAT_ILLNESS:
                return true;

            case CAT_ENFANT_MALADE:
                return true;

            case CAT_ACCIDENT_TRAVAIL:
                return true;

            case CAT_GREV:
                return true;

            case CAT_NON_PLANNIFIABLE:
                return true;

            case CAT_ABSENCE_A_JUSTIFIER:
                return true;

            // ToSync compatibility
            case "REPOS":
                return true;

            default:
                return false;
        }
    }

    /**
     * compute all flight hours between 21h and 09h local time
     */
    private void computeHCNuit() {

        long nightTimeinMillis = 0;

        int offset = TimeZone.getTimeZone("Europe/Paris").getOffset(gcBegin.getTimeInMillis());
        // create calendars set at 9h00 and 21h00 (Paris local time)
        GregorianCalendar gc9loc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gc9loc.set(
                gcBegin.get(Calendar.YEAR),
                gcBegin.get(Calendar.MONTH),
                gcBegin.get(Calendar.DAY_OF_MONTH),
                9,
                0,
                0);
        gc9loc.set(Calendar.MILLISECOND, 0);
        gc9loc.setTimeInMillis(gc9loc.getTimeInMillis() - offset);

        GregorianCalendar gc21loc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gc21loc.set(
                gcBegin.get(Calendar.YEAR),
                gcBegin.get(Calendar.MONTH),
                gcBegin.get(Calendar.DAY_OF_MONTH),
                21,
                0,
                0);
        gc21loc.set(Calendar.MILLISECOND, 0);
        gc21loc.setTimeInMillis(gc21loc.getTimeInMillis() - offset);

        // compute hours before 9h00LT
        if (gcBegin.getTimeInMillis() < gc9loc.getTimeInMillis()) {
            nightTimeinMillis = Math.min(
                    gc9loc.getTimeInMillis() - gcBegin.getTimeInMillis(),
                    gcEnd.getTimeInMillis() - gcBegin.getTimeInMillis());
            if (gcEnd.getTimeInMillis() > gc21loc.getTimeInMillis()) {
                nightTimeinMillis += gcEnd.getTimeInMillis() - gc21loc.getTimeInMillis();
            }
            hvNuit = nightTimeinMillis / 3600000f;
            hcNuit = hvNuit * COEFF_HCNUIT;
            return;
        }
        // compute hours after 21h00LT
        if (gcBegin.getTimeInMillis() > gc9loc.getTimeInMillis()) {
            if (gcEnd.getTimeInMillis() > gc9loc.getTimeInMillis() + ONE_DAY_IN_MILLISECONDS) {
                nightTimeinMillis = Math.min(
                        gc9loc.getTimeInMillis() + ONE_DAY_IN_MILLISECONDS - gcBegin.getTimeInMillis(),
                        gc9loc.getTimeInMillis() + ONE_DAY_IN_MILLISECONDS - gc21loc.getTimeInMillis());
            } else if (gcEnd.getTimeInMillis() > gc21loc.getTimeInMillis()) {
                nightTimeinMillis = Math.min(
                        gcEnd.getTimeInMillis() - gcBegin.getTimeInMillis(),
                        gcEnd.getTimeInMillis() - gc21loc.getTimeInMillis());
            }

            hvNuit = nightTimeinMillis / 3600000f;
            hcNuit = hvNuit * COEFF_HCNUIT;
        }
    }

    public boolean isFirstEventOfDay() {
        return firstEventOfDay;
    }

    public void setFirstEventOfDay(boolean firstEventOfDay) {
        this.firstEventOfDay = firstEventOfDay;
    }

    public String getDateString() {
        return dateString.get();
    }

    public String getActivityString() {
        return activityString.get();
    }

    public String getFunction() {
        return function;
    }

    public ArrayList<CrewMember> getAlCrewMembers() {
        return crew;
    }

    public void setCrewMembers(ArrayList<CrewMember> crewMembers) {
        this.crew = crewMembers;
    }

    public float getBlockTime() {
        return blockTime;
    }

    public ObjectProperty<GregorianCalendar> getDateProperty() {
        return dateProperty;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getHotelData() {
        return hotelData;
    }

    public void setHotelData(String hotelData) {
        this.hotelData = hotelData;
    }

    public String getTraining() {
        return training;
    }

    public void setTraining(String training) {
        this.training = training;
    }

    public boolean isSimActivity() {
        if (Arrays.stream(PAID_SIM_ACTIVITY).anyMatch(category::equals)) {
            return true;
        } else {
            return false;
        }

//        switch (category) {
//            case CAT_SIMU:
//                return true;
//            case CAT_SIM_C1:
//                return true;
//            case CAT_SIM_C2:
//                return true;
//            case CAT_SIM_E1:
//                return true;
//            case CAT_SIM_E2:
//                return true;
//            case CAT_SIM_LOE:
//                return true;
//            case CAT_SIM_LVO:
//                return true;
//            case CAT_SIM_ENT1:
//                return true;
//            case CAT_SIM_UPRT:
//                return true;
//
//            default:
//                return false;
//        }
    }

    public boolean isAcceptedCrewRequest() {
        return acceptedCrewRequest;
    }

    public void setAcceptedCrewRequest(boolean acceptedCrewRequest) {
        this.acceptedCrewRequest = acceptedCrewRequest;
    }

    public boolean isExportable() {
        return exportable;
    }

    public void setExportable(boolean exportable) {
        this.exportable = exportable;
    }

    public float getHvNuit() {
        return hvNuit;
    }

    public float getHcNuit() {
        return hcNuit;
    }

    public void setTooltipHcContent(String tooltipHcContent) {
        this.tooltipHcContent = tooltipHcContent;
    }

    public String getTooltipHcContent() {
        return tooltipHcContent;
    }

    public long getFdpTime() {
        return fdpTime;
    }

    public void setFdpTime(long fdpTime) {
        this.fdpTime = fdpTime;
    }

    public float getHv() {
        return hv;
    }

    public void setHv(float hv) {
        this.hv = hv;
    }

    public float getHv_r() {
        return hv_r;
    }

    public void setHv_r(float hv_r) {
        this.hv_r = hv_r;
    }

    public String[] getCrewCab() {
        return crewCab;
    }

    public String[] getCrewTec() {
        return crewTec;
    }

    public String[] getCrewDhd() {
        return crewDhd;
    }

    public void setCrewCab(String[] crewCab) {
        this.crewCab = crewCab;
    }

    public void setCrewTec(String[] crewTec) {
        this.crewTec = crewTec;
    }

    public void setCrewDhd(String[] crewDhd) {
        this.crewDhd = crewDhd;
    }

    public String[] getCrewGnd() {
        return crewGnd;
    }

    public void setCrewGnd(String[] crewGnd) {
        this.crewGnd = crewGnd;
    }

    public String[] getCrewList() {
        return crewList;
    }

    public void setCrewList(String[] crewList) {
        this.crewList = crewList;
    }

    public String getCheckInUtc() {
        return checkInUtc;
    }

    public void setCheckInUtc(String checkInUtc) {
        this.checkInUtc = checkInUtc;
    }

    public String getCheckInLoc() {
        return checkInLoc;
    }

    public void setCheckInLoc(String checkInLoc) {
        this.checkInLoc = checkInLoc;
    }

    public String getBlkFdpDuty() {
        return blkFdpDuty;
    }

    public void setBlkFdpDuty(String blkFdpDuty) {
        this.blkFdpDuty = blkFdpDuty;
    }

    public String getsTotalBlock() {
        return sTotalBlock;
    }

    public void setsTotalBlock(String sTotalBlock) {
        this.sTotalBlock = sTotalBlock;
    }

    public String getsFlightDutyPeriod() {
        return sFlightDutyPeriod;
    }

    public void setsFlightDutyPeriod(String sFlightDutyPeriod) {
        this.sFlightDutyPeriod = sFlightDutyPeriod;
    }

    public String getsDutyTime() {
        return sDutyTime;
    }

    public void setsDutyTime(String sDutyTime) {
        this.sDutyTime = sDutyTime;
    }
}
