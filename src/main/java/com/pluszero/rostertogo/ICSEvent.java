package com.pluszero.rostertogo;

import com.pluszero.rostertogo.model.PlanningEvent;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ICSEvent {

    // Debug flag to avoid creating Log.v objects if unnecessary
    private static final String TAG = "ICSEvent";
    public static final String TAG_BEGIN = "BEGIN:VEVENT";
    private final static String TAG_REQUEST_TIME = "UID:";
    private final static String TAG_STATUS = "STATUS:";
    private final String TAG_START_TIME = "DTSTART;VALUE=DATE-TIME:";
    private final String TAG_END_TIME = "DTEND;VALUE=DATE-TIME:";
    private final String TAG_START_DATE = "DTSTART;VALUE=DATE:";    //tosync compatibility
    private final String TAG_END_DATE = "DTEND;VALUE=DATE:";        //tosync compatibility

    private final String TAG_CATEGORIES = "CATEGORIES:";
    private final String TAG_SUMMARY = "SUMMARY:";
    private final String TAG_DESCRIPTION = "DESCRIPTION:";
    public static final String TAG_END = "END:VEVENT";

    private String ICSText = "";
    private String category = "";
    private String iataDepart = "";
    private String iataArrivee = "";

    public ICSEvent(String ICSData) {
        ICSText = ICSData;
        category = getICSCategory();
    }

    public GregorianCalendar getICSStart() {
        // search DATE-TIME if any
        String target = TAG_START_TIME + "(\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2}Z)";
        Pattern regex = Pattern.compile(target);
        Matcher result = regex.matcher(ICSText);
        if (result.find()) {
            String date = result.group(1);
            GregorianCalendar cal = getDateZ(date);
            return cal;
        }

        // search DATE if any
        target = TAG_START_DATE + "(\\d{8})";
        regex = Pattern.compile(target);
        result = regex.matcher(ICSText);
        if (result.find()) {
            String date = result.group(1);
            GregorianCalendar cal = getDateZ(date);
            return cal;
        }
        // if nothing found
        return null;
    }

    public GregorianCalendar getICSEnd() {
        String target = TAG_END_TIME + "(\\d{4}\\d{2}\\d{2}T\\d{2}\\d{2}\\d{2}Z)";

        Pattern regex = Pattern.compile(target);
        Matcher result = regex.matcher(ICSText);
        if (result.find()) {
            String date = result.group(1);
            GregorianCalendar cal = getDateZ(date);
            return cal;
        }

        // search DATE if any
        target = TAG_END_DATE + "(\\d{8})";
        regex = Pattern.compile(target);
        result = regex.matcher(ICSText);
        if (result.find()) {
            String date = result.group(1);
            GregorianCalendar cal = getDateZ(date);
            return cal;
        }

        // if nothing found
        return null;
    }

    public String getICSStatus() {
        // use \r\n|\r\n instead of System.lineSeparator() cause server sends
        // \r\n that is not recognised by MacOS \n line separator 
        String cible = TAG_STATUS + "(.*)" + "(\r\n|\r|\n)";
        Pattern regex = Pattern.compile(cible);
        Matcher result = regex.matcher(ICSText);
        if (result.find()) {
            return result.group(1);
        } else {
            return "NOSTATUS";
        }
    }

    public String getICSCategory() {
        // use \r\n|\r\n instead of System.lineSeparator() cause server sends
        // \r\n that is not recognised by MacOS \n line separator 
        String cible = TAG_CATEGORIES + "(.*)" + "(\r\n|\r|\n)";
        Pattern regex = Pattern.compile(cible);
        Matcher result = regex.matcher(ICSText);
        if (result.find()) {
            return result.group(1);
        } else {
            return "NOCAT";
        }
    }

    public String getICSDesc() {
        // use \r\n|\r\n instead of System.lineSeparator() cause server sends
        // \r\n that is not recognised by MacOS \n line separator 
        String cible = TAG_DESCRIPTION + "(.*)" + "(\r\n|\r|\n)";
        Pattern regex = Pattern.compile(cible);
        Matcher result = regex.matcher(ICSText);
        if (result.find()) {
            String desc = result.group(1).replace("\\n", "\n");
            StringBuilder str = new StringBuilder();
            str.append(desc);
            return str.toString();
        } else {
            return "-NIL-";
        }
    }

    public String getICSSummary() {
        // use \r\n|\r\n instead of System.lineSeparator() cause server sends
        // \r\n that is not recognised by MacOS \n line separator 
        String cible = TAG_SUMMARY + "(.*)" + "(\r\n|\r|\n)";
        Pattern regex = Pattern.compile(cible);
        Matcher result = regex.matcher(ICSText);
        if (result.find()) {
            String summary = result.group(1);
            if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(category::equals)) {
                extractAirports(summary);
            }

            return summary;
        } else {
            return "No Summary";
        }
    }

    private void extractAirports(String summary) { // SUMMARY:TO3082
        // ORY-SAW(+0300) or GPM*GNT for deadheading
        String cible = "([A-Z]{3})[-\\*]([A-Z]{3})";
        Pattern regex = Pattern.compile(cible);
        Matcher result = regex.matcher(summary);
        if (result.find()) {
            iataDepart = result.group(1).toLowerCase();
            iataArrivee = result.group(2).toLowerCase();
            return;
        }
        // ToSync compatibility
        cible = "([A-Z]{3}) [-\\*] ([A-Z]{3})";
        regex = Pattern.compile(cible);
        result = regex.matcher(summary);
        if (result.find()) {
            iataDepart = result.group(1).toLowerCase();
            iataArrivee = result.group(2).toLowerCase();
        }
    }

    private GregorianCalendar getDateZ(String date) { // 20140605T123000Z
        GregorianCalendar tmp = new GregorianCalendar();
        tmp.setTimeZone(TimeZone.getTimeZone("UTC"));
        return getDate(date, tmp);

    }

    private GregorianCalendar getDate(String date, GregorianCalendar tmp) { // 20140605T123000Z

        String cible = "(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})Z";
        Pattern regex1 = Pattern.compile(cible);
        Matcher result1 = regex1.matcher(date);
        if (result1.find()) {
            // add -1 to group(2) cause GregorianCalendar stores months from 0 to 11
            tmp.set(Integer.valueOf(result1.group(1)), Integer.valueOf(result1.group(2)) - 1, Integer.valueOf(result1.group(3)), Integer.valueOf(result1.group(4)),
                    Integer.valueOf(result1.group(5)), Integer.valueOf(result1.group(6)));
            // set seconds and milliseconds to zero
            tmp.set(Calendar.SECOND, 0);
            tmp.set(Calendar.MILLISECOND, 0);
        } else {
            // ToSync compatibility
            cible = "(\\d{4})(\\d{2})(\\d{2})";
            regex1 = Pattern.compile(cible);
            result1 = regex1.matcher(date);
            if (result1.find()) {
                // add -1 to group(2) cause GregorianCalendar stores months from 0 to 11
                tmp.set(Integer.valueOf(result1.group(1)), Integer.valueOf(result1.group(2)) - 1, Integer.valueOf(result1.group(3)), 0, 0, 0);
            }
        }

        return tmp;
    }
}
