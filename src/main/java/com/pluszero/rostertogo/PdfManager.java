/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo;

/**
 *
 * @author Cyril
 */
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfManager {

    public static final String CHECK_IN_UTC = "(Check In \\(UTC\\) : \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2})";
    public static final String CHECK_IN_LOC = "(Briefing \\(ORY LT\\) : \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2})";
    public static final String BLK_FDP_DUTY = "(TOTAL BLK=\\d{1,2}:\\d{2} FDP=\\d{1,2}:\\d{2}\\[\\d{1,2}:\\d{2}\\]\\s+DUTY=\\d{1,2}:\\d{2})";
    public static final String TOTAL_BLK = "(TOTAL BLK=\\d{1,2}:\\d{2})";
    public static final String FDP = "(FDP=\\d{1,2}:\\d{2}\\[\\d{1,2}:\\d{2}\\])";
    public static final String DUTY = "(DUTY=\\d{1,2}:\\d{2})";

    public static final String TYPE_PILOT = "Pilot:";
    public static final String TYPE_CABIN = "Cabin:";
    public static final String TYPE_DEADHEAD = "DHD:";
    public static final String TYPE_GROUND = "GND:";

    public ArrayList<String> alPdfEvents;
    private ArrayList<String> alHotels;
    private final DateFormatSymbols dfs_fr = new DateFormatSymbols();
    private final DateFormatSymbols dfs_en = new DateFormatSymbols();

    private final String[] shortDays_fr = new String[]{"", "dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam."};
    private final String[] shortDays_en = new String[]{"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private final SimpleDateFormat sdf_fr;
    private final SimpleDateFormat sdf_en;

    private final String newline = System.lineSeparator();

    public PdfManager() {
        dfs_fr.setShortWeekdays(shortDays_fr);
        dfs_en.setShortWeekdays(shortDays_en);
        sdf_fr = new SimpleDateFormat("E dd/MM/yyyy", dfs_fr);
        sdf_en = new SimpleDateFormat("E MM/dd/yyyy", dfs_en);

        alPdfEvents = new ArrayList<>();
    }

    public void extractPdfData(String content) throws Exception {
        MainApp.userPrefs.crewFunction = extractCrewFunction(content);
        String cleanedContent = cleanPdf(content);
        stripPdf(cleanedContent);
        alHotels = buildHotelDetailsList(cleanedContent);
    }

    private void stripPdf(String content) {
        // build an array of lines
        String[] lines = content.split("\\r?\\n");
        StringBuilder block = new StringBuilder();
        Pattern regex_fr = Pattern.compile("[a-z]{3}\\. [0-9]{2}/[0-9]{2}/[0-9]{4}");
        Pattern regex_en = Pattern.compile("[A-Z]{1}[a-z]{2} [0-9]{2}/[0-9]{2}/[0-9]{4}");

        for (String line : lines) {
            Matcher result_fr = regex_fr.matcher(line);
            Matcher result_en = regex_en.matcher(line);

            if (result_fr.find() || result_en.find()) {
                if (block.length() > 0) {
                    if (block.indexOf("TOTAL BLK=") != -1) {
                        alPdfEvents.add(fixSplittedBlkFdpDuty(block.toString()));
                    } else {
                        alPdfEvents.add(block.toString());
                    }
                }
                block.setLength(0); // reset the stringbuilder
                block.append(line
                        .replaceAll("(?<!\\\\)\\(", "")
                        .replaceAll("(?<!\\\\)\\)", "")
                        .replaceAll("\\\\\\)", ")").replaceAll("\\\\\\(", "(")
                        .replace("Tj", "").trim()).append(newline);
            } else {
                if (block.length() > 0) {
                    block.append(line
                            .replaceAll("(?<!\\\\)\\(", "")
                            .replaceAll("(?<!\\\\)\\)", "")
                            .replaceAll("\\\\\\)", ")").replaceAll("\\\\\\(", "(")
                            .trim()).append(newline);
                }
            }
        }
        // add the last built block as no more lines are available
        if (block.length() > 0) {
            alPdfEvents.add(block.toString());
        }

        // remove the 'false' blocks containing 'Crew Roster by Period' without
        // containing 'Begin duty'     
        // create a new arraylist with valid elements (not containing 'Crew 
        // roster by period'. It's better than the remove() option
        ArrayList<String> events = new ArrayList<>();
        for (int i = 0; i < alPdfEvents.size(); i++) {
            if (!alPdfEvents.get(i).contains("Crew Roster by Period")) {
                events.add(alPdfEvents.get(i));
            }

            if (alPdfEvents.get(i).contains("Crew Roster by Period")) {
                if (alPdfEvents.get(i).contains("Begin Duty")) {
                    events.add(alPdfEvents.get(i));
                }
            }
        }
        alPdfEvents = events;
    }

    /**
     * rebuilt the last line in remarks column when it's splitted (DUTY=XX:XX or
     * XX:XX)
     *
     * @param src
     */
    private String fixSplittedBlkFdpDuty(String src) {
        String[] lines = src.split("\\r?\\n");
        if (lines[lines.length - 2].startsWith("TOTAL BLK=")) {
            lines[lines.length - 2] += lines[lines.length - 1];
            lines[lines.length - 1] = null;
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line != null) {
                sb.append(line).append(newline);
            }
        }
        return sb.toString();
    }

    /**
     * Scan the array of strings for the matching date, and get the sim crew
     *
     * @param cal the date of the activity
     * @return the crew/participant of the activity
     */
    public String findTraining(GregorianCalendar cal) {

        // get each part of pdf between indices and parse it
        for (int i = 0; i < alPdfEvents.size(); i++) {
            String s = alPdfEvents.get(i);
            // detect if part is the matching date
            if (s.contains(sdf_fr.format(cal.getTime())) || s.contains(sdf_en.format(cal.getTime()))) {
                if (s.contains("Ground Act.") && s.toLowerCase().contains("simu")) {
                    return extractTraining(s);
                }
            }
        }
        return null;
    }

    /**
     * Scan the array of strings for the matching date, departure and
     * destination, and get the remarks
     *
     * @param cal the date of the activity
     * @param dep the airport of departure
     * @param arr the airport of arrival
     * @return the remarks of the activity
     */
    public String findRemarks(GregorianCalendar cal, String dep, String arr) {
        // get each part of pdf between indices and parse it
        String remarks = "";

        for (int i = 0; i < alPdfEvents.size(); i++) {
            String s = alPdfEvents.get(i);
            // detect if part is the matching date, and if the string is long
            // enough to contain all data and not just date (case of PDF line break)
            if ((s.contains(sdf_fr.format(cal.getTime())) || s.contains(sdf_en.format(cal.getTime()))) && s.length() > 64) {
                if ((s.contains("Duty Flight") && s.contains(dep + "-" + arr)) || s.contains("Simu")) {
                    if (s.contains("Check In")) {
                        remarks = s.substring(s.indexOf("Check In"));
                    } else if (s.contains("TOTAL BLK=")) {
                        remarks = s.substring(s.indexOf("TOTAL BLK="));
                    } else if (s.contains("Briefing")) {
                        remarks = s.substring(s.indexOf("Briefing"));
                    }

                    if (remarks.contains("Blk Hrs")) {
                        remarks = remarks.substring(0, remarks.indexOf("Blk Hrs"));
                    }
                }
                return remarks;
            }
        }
        return remarks;
    }

    /**
     * Scan the array of strings for the matching date, and get the remarks
     *
     * @param cal the date of the activity
     * @return the remarks of the activity
     */
    public String findRemarks(GregorianCalendar cal) {
        StringBuilder sb = new StringBuilder();
        // get each part of pdf between indices and parse it
        for (int i = 0; i < alPdfEvents.size(); i++) {
            String s = alPdfEvents.get(i);
            // detect if part is the matching date
            if ((s.contains(sdf_fr.format(cal.getTime())) || s.contains(sdf_en.format(cal.getTime()))) && s.length() > 64) {
                sb.append(s.substring(s.indexOf("Check In")));
                if (sb.indexOf("Blk Hrs") != -1) {
                    sb.setLength(sb.indexOf("Blk Hrs"));
                }
                return sb.toString();
            }
        }
        return null;
    }

    public String findHotelDetails(GregorianCalendar cal) {
        if (alHotels == null || alHotels.isEmpty()) {
            return null;
        }
        for (int i = 0; i < alPdfEvents.size(); i++) {
            String s = alPdfEvents.get(i);

            // detect if part is the matching date
            if (s.contains(sdf_fr.format(cal.getTime())) || s.contains(sdf_en.format(cal.getTime()))) {
                // search for hotel info
                for (String hotel : alHotels) {
                    // test if hotel name commes before "hotel details"
                    if (s.indexOf(hotel) > s.indexOf("Hotel Details")) {
                        continue;
                    }
                    // search for first 10 chars cause hotel details
                    //  include telephone number and adress
                    if (s.contains(hotel.substring(0, 10))) {
                        return hotel;
                    }
                }
            }
        }
        return null;
    }

    private String extractTraining(String s) {

        // split the source in lines
        String[] array = s.split(newline);
        // find line number of "Ins."
        int begin = 0;
        int end = 0;

        if (s.contains("Ins.")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].contains("Ins.")) {
                    begin = i;
                    continue;
                }

                if (array[i].contains("Check In")) {
                    end = i;
                    break;
                }
            }
        } else if (s.contains("Tr.")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].contains("Tr.")) {
                    begin = i;
                    continue;
                }

                if (array[i].contains("Check In")) {
                    end = i;
                    break;
                }
            }
        } else {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        // add the line juste above "Ins."
        sb.append("Training : ").append(array[begin - 1].trim()).append(newline);
        // add the rest
        for (int i = begin; i < end; i++) {
            // to deal with the case of LOE sim mismatched with LOE trigram
            // check if line equals "LOE" before trimming/adding 
            if (!array[i].equals("LOE")) {
                //TODO parse sim attendants if decoded
                sb.append(array[i]).append(newline);
            }
        }
        return sb.toString();
    }

    private ArrayList<String> buildHotelDetailsList(String src) {
        ArrayList<String> list = new ArrayList<>();
        String target = "Hotel Telephone Address";
        if (!src.contains(target)) {
            return list;
        }
        int idx = src.indexOf(target) + target.length() + newline.length();

        String[] lines = src.substring(idx).split(newline);
        for (int i = 0; i < lines.length; i++) {
            if (i == 0 && (lines[i].equals(" ") || lines[i].equals(""))) {
                continue;
            }
            // deal with page change
            if (lines[i].contains(target)) {
                continue;
            }
            if (lines[i].contains("Commander = ")) {
                continue;
            }
            if (lines[i].contains("Crew Roster")) {
                continue;
            }
            if (lines[i].contains("Schedule in")) {
                continue;
            }
            if (lines[i].contains("Licenced to")) {
                continue;
            }
            if (lines[i].contains("Printed on")) {
                continue;
            }
            if (lines[i].contains(" / Box ")) {
                continue;
            }
            if (lines[i].contains("Page ")) {
                continue;
            }
            // end of hotel details area
            if (i > 0 && (lines[i].equals(" ") || lines[i].equals(""))) {
                break;
            }
            if (lines[i].contains("Remarks")) {
                break;
            }
            if (lines[i].contains("Applicability Remark")) {
                break;
            }
            list.add(lines[i]);
        }

        return list;
    }

    /*
    remove irrelevant lines from PDF
     */
    private String cleanPdf(String source) {
        // do not filter "Crew Roster by Period", as it is a tag to delete
        // false blocks caused by page break

        String[] starters = new String[]{
            "Printed on",
            "Commander =",
            "Page ",
            "Type From To Activity Fct Routing",
            ">>>",
            "==>",
            "End Rest"};

        String[] lineValue = new String[]{
            "Schedule in UTC",
            "Licenced to transavia.com France",
            "Type",
            "From",
            "To",
            "Activity",
            "Fct",
            "Routing / Description",
            "A/C",
            "Blk",
            "Ground Act.",
            "Begin Duty",
            "End Duty",
            "Standard Duty",
            "End Rest"
        };
        String[] array = source.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        outerloop:
        for (String line : array) {
            if (!line.endsWith(" Tj")) {
                continue;
            }
            for (String s : starters) {
                if (line.startsWith("(" + s)) {
                    continue outerloop;
                }
            }

            for (String s : lineValue) {
                if (line.equals("(" + s + ") Tj")) {
                    continue outerloop;
                }
            }

            // deal with "XXX Surname NAME XXX / Box employeeID"
            if (line.matches("[A-Z]{3}.+[A-Z]{3} / Box [0-9]+")) {
                continue;
            }
            String s = line.substring(1, line.lastIndexOf(") Tj"));
            sb.append(s.replaceAll("\\s{2,}", " ")).append(System.lineSeparator());

        }

        return sb.toString();
    }

    public String[] extractCrewList(String src) {
        String[] values = new String[]{"Pilot:", "Cabin:", "DHD:", "GND:"};
        Pattern regex = Pattern.compile("^(\\W?[A-Z]+[a-zA-Z]+\\s[[A-Z]+[a-zA-Z]+\\s]+\\([A-Z]{3}\\))$");

        String[] array = src.split("\\r?\\n");
        ArrayList<String> alCrewList = new ArrayList<>();

        for (String line : array) {
            for (String val : values) {
                if (line.equals(val)) {
                    alCrewList.add(line);
                }
            }
            Matcher result = regex.matcher(line);
            if (result.find()) {
                alCrewList.add(line);
            }
        }

        if (!alCrewList.isEmpty()) {
            return alCrewList.toArray(String[]::new);
        } else {
            return null;
        }
    }

    public String[] extractCrewType(String[] array, String type) {
        // LinkedHashSet to keep insertion order of elements
        LinkedHashSet<String> set = new LinkedHashSet<>();
        String target = type;
        boolean flag = false;
        int idx = 0;
        // first find the index matching the target
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target)) {
                idx = i;
                flag = true;
                break;
            }
        }
        // get out if no target found
        if (!flag) {
            return null;
        }
        // now scan the array from the index
        for (int i = idx; i < array.length; i++) {
            // break the loop if another type is found
            if ((array[i].equals(TYPE_PILOT) || array[i].equals(TYPE_CABIN) || array[i].equals(TYPE_DEADHEAD) || array[i].equals(TYPE_GROUND)) && !array[i].equals(target) && flag) {
                break;
            }

            if (!array[i].equals(target) && flag) {
                set.add(reformatCrewData(array[i]));
            }
        }
        return set.toArray(String[]::new);
    }

    public int extractCrewFunction(String src) {
        // search for something like CDB / Box 123456
        Pattern regex = Pattern.compile("(([A-Z]{3})\\s/\\s\\bBox\\b)");
        Matcher result = regex.matcher(src);
        if (result.find()) {
            return UserPrefs.crewFunctionToInt(result.group(2));
        }
        return UserPrefs.FUNCTION_UNKNOWN;
    }

    /*
    looks for CheckIn times (UTC and LT), Block / FDP / Duty
     */
    public String extractAdditionalTimeData(String src, String pattern) {
        Pattern regex = Pattern.compile(pattern);
        Matcher result = regex.matcher(src);
        if (result.find()) {
            return result.group(1);
        }
        return null;
    }

    public String findPdfEvent(GregorianCalendar cal) {
        for (int i = 0; i < alPdfEvents.size(); i++) {
            String s = alPdfEvents.get(i);
            // detect if part is the matching date, and if the string is long
            // enough to contain all data and not just date (case of PDF line break)
            if ((s.contains(sdf_fr.format(cal.getTime())) || s.contains(sdf_en.format(cal.getTime()))) && s.length() > 64) {
                return s;
            }
        }
        return null;
    }

    private String reformatCrewData(String crewData) {

        Pattern regex = Pattern.compile("(\\([A-Z]{3}\\))");

        // put the trigramme at the beginning and remove parenthesis
        Matcher result = regex.matcher(crewData);
        if (result.find()) {
            StringBuilder sb = new StringBuilder();
            sb.append(result.group(1).replace("(", "").replace(")", ""))
                    .append(": ")
                    .append(crewData.substring(0, crewData.indexOf("(")));
            Pattern p = Pattern.compile("[^a-z]", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(sb.toString().substring(5, 6));
            if (m.find()) {
                sb.deleteCharAt(5);
            }
            return sb.toString().trim();
        }
        return null;
    }
}
