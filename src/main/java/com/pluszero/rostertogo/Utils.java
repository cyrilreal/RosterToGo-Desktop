package com.pluszero.rostertogo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

public class Utils {

    public static final String SAVE_DIRECTORY = "CrewTO";

    public static final int FILE_WRITE_OK = 0;
    public static final int FILE_WRITE_ERROR = -1;

    public static String getDateTimeExtension() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Calendar cal = Calendar.getInstance();
        String tmp = dateFormat.format(cal.getTime());
        return tmp;
    }

    public static String getConnexionDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm");
        Date date = Calendar.getInstance().getTime();
        String jour = dateFormat.format(date);
        String heure = hourFormat.format(date);

        String tmp = " le " + jour + " � " + heure;
        return tmp;
    }

    public static boolean isWellFormed(InputStream is) {
        DefaultHandler handler = new DefaultHandler();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(is, handler);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Parse a string formatted as "HH.MM" and returns a Calendar object set
     * with the time.
     *
     * @param time The string to parse
     * @return The newly created Calendar object with the parsed time.
     */
    public static Calendar getCalendar(String time, TimeZone timeZone) {
        Calendar calendar = new GregorianCalendar(timeZone);

        String[] timeComponents = time.split(":");
        setTime(calendar, Integer.parseInt(timeComponents[0]), Integer.parseInt(timeComponents[1]), 0, 0);
        return calendar;
    }

    /**
     * Set the time of the {@code calendar}.
     *
     * @param calendar The calendar to which to set the time
     * @param hour The hour to set
     * @param minute The minute to set
     * @param second The second to set
     * @param millisecond The millisecond to set
     */
    public static void setTime(Calendar calendar, int hour, int minute, int second, int millisecond) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);
    }

    private static class FilesCompare implements Comparator<File> {

        @Override
        public int compare(File lhs, File rhs) {
            return (int) (((File) lhs).lastModified() - ((File) rhs).lastModified());
        }

    }

    public static String msToHours(long ms) {
        final long MILLIS_PER_SECOND = 1000;
        final long SECONDS_PER_HOUR = 3600;
        final long SECONDS_PER_MINUTE = 60;

        long deltaSeconds = ms / MILLIS_PER_SECOND;
        long deltaHours = deltaSeconds / SECONDS_PER_HOUR;
        long leftoverSeconds = deltaSeconds % SECONDS_PER_HOUR;
        long deltaMinutes = leftoverSeconds / SECONDS_PER_MINUTE;

        return deltaHours + ":" + deltaMinutes;
    }

    // Extrait une sous chaine de string
    public static String extractString(String src, String debut, String fin) {
        return extractString(src, debut, fin, 0);
    }

    public static String extractString(String src, String debut, String fin, int startIndex) {
        int i = 0;
        int j = 0;

        i = src.indexOf(debut, startIndex);
        if (i == -1) {
            return null;
        }
        j = src.indexOf(fin, i);
        if (j == -1) {
            return null;
        }

        String tmp = null;
        if (i < j + fin.length()) // tmp = src.substring(i, j - 1);
        {
            tmp = src.substring(i, j + fin.length());
        }

        return tmp;
    }

    public static String getWindowId(String location) {
        // Extrait le code 3 chiffre window id de l'url
        String cible = "windowId=(\\w{3})";
        Pattern regex1 = Pattern.compile(cible);
        Matcher result1 = regex1.matcher(location);

        if (result1.find()) {
            return result1.group(1);
        } else {
            return null;
        }
    }

    public static int countEvents(String ics) {
        // Extrait le code 3 chiffre window id de l'url
        String cible = "BEGIN:VEVENT";
        Pattern regex1 = Pattern.compile(cible);
        Matcher result1 = regex1.matcher(ics);
        int i = 0;
        while (result1.find()) {
            i++;
        }
        return i;
    }

    public static String convertMinutesToHoursMinutes(int timeInMinutes) {
        int h = timeInMinutes / 60;
        int m = timeInMinutes % 60;

        return h + "h" + (m < 10 ? "0" + m : m);
    }

    public static float convertMillisecondsToDecimalHours(long milliseconds) {
        return (float) milliseconds / 3600000;
    }

    public static String convertDecimalHourstoHoursMinutes(float decimalHours) {
        int h = (int) decimalHours;
        int m = Math.round((decimalHours % 1) * 60);

        return h + "h" + (m < 10 ? "0" + m : m);
    }

    public static boolean deleteDirectory(File path) {
        boolean resultat = true;

        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    resultat &= deleteDirectory(files[i]);
                } else {
                    resultat &= files[i].delete();
                }
            }
        }
        resultat &= path.delete();
        return resultat;
    }

    /**
     * split a string and trim each part
     *
     * @param source the string to split
     * @param separator the regex on which to split
     * @return String
     */
    public static String splitTrim(String source, String separator) {
        String[] array = source.split(separator);
        StringBuilder sb = new StringBuilder();
        for (String string : array) {
            sb.append(string.trim()).append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }

    public static StringBuilder readFile(File selectedFile) {
        StringBuilder sb = new StringBuilder(1024);
        String curLine = "";
        try {
            FileReader fr = new FileReader(selectedFile);
            BufferedReader br = new BufferedReader(fr);

            while (curLine != null) {
                curLine = br.readLine();
                sb.append(curLine).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.getMessage();
        }
        sb.setLength(sb.length() - System.lineSeparator().length());
        return sb;
    }

    public static int saveFile(String content, File file) {
        FileWriter fw = null;
        try {
            Path path = Paths.get(file.getPath()).getParent();
            // test existence of application directory
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            file.createNewFile();
            fw = new FileWriter(file);
            fw.write(content);
        } catch (IOException ex) {
            return FILE_WRITE_ERROR;
        } finally {
            try {
                if (fw != null) {
                    fw.flush();
                    fw.close();
                }
            } catch (IOException ex) {
            }
        }
        return FILE_WRITE_OK;
    }

    public static String detectOsAppFolder() {
        String os = System.getProperty("os.name").toUpperCase();
        if (os.contains("WIN")) {
            return System.getenv("APPDATA") + File.separator + "RosterToGo";
        }
        if (os.contains("MAC")) {
            return System.getProperty("user.home") + File.separator + "Library"
                    + File.separator + "Application Support" + File.separator
                    + "RosterToGo";
        }
        if (os.contains("NUX")) {
            return System.getProperty("user.dir") + File.separator + ".RosterToGo";
        }
        return null;
    }

    public static String stringFromInputStream(InputStream is) {

        // read it with BufferedReader
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            br.close();

        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
