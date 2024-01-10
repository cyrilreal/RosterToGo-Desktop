/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo;

import com.pluszero.rostertogo.model.DutyPeriod;
import com.pluszero.rostertogo.model.MonthActivity;
import com.pluszero.rostertogo.model.ReferenceTime;
import com.pluszero.rostertogo.model.PlanningEvent;
import com.pluszero.rostertogo.model.PlanningModel;
import com.pluszero.rostertogo.model.Rotation;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

/**
 *
 * @author Cyril
 */
public class ActivityAnalyser {

    public static final int PAY_MODE_TO = 0;
    public static final int PAY_MODE_AF = 1;

    private static final int TEN_HOURS = 36000000; // 10 hours in milliseconds
    private final ArrayList<MonthActivity> alMonths;
    private final PlanningModel model;
    private final ArrayList<Rotation> alRotations;
    private String crewMemberBase = "ORY";

    private float hvNuit = 0.0f;

    private static final float FIXED_PAY_CAPTAIN_ECHELON_1 = 2295.60f;  // AF only
    private static final float BASIC_PV = 71.68f;                       // AF mode only
    public float pvei = 0.0f;                                           // AF mode only
    private float pvSupRate = 0.0f;                                     // AF mode only
    public float fixedPayAF;                                            // AF mode only
    public float minimumGuaranteedPayAF;                                 // AF mode only

    public float fixedPayTO, pvNormTO, pvSup75hcTO, minimumGuaranteedPayTO;

    private ArrayList<ReferenceTime> alPaidTimes;
    private final int payMode;

    private int crewFunction;

    private final String[] legsSup2100NM = {"ORY-CSK", "CSK-ORY", "NTE-DSS", "DSS-NTE", "ORY-ETM", "ETM-ORY"};

    public ActivityAnalyser(PlanningModel model, int crewFunction, int payMode) {

        this.model = model;
        this.crewFunction = crewFunction;
        this.payMode = payMode;
        detectCrewMemberBase();
        alMonths = buildMonthsList();
        alRotations = new ArrayList<>();
        initTempsReference();
        computeMonthBlockHours();
        if (MainApp.userPrefs.autoComputeCreditHours) {
            computeMonthsHcs(payMode);
            computeHVandHV_r();
            if (!modelHasFlights()) {
                return;
            }
            buildRotations();
            computeRotationsCreditHours(payMode);
            computeMonthsFlightCreditHours(payMode);
            computeMonthVacationAndIllnessDays();
            computeMonthDutyDays();
            computeMonthNumberOfDutyLegs();
            computeMonthsCreditHours(payMode);
            computeMonthsPv(payMode);
//            if (payMode == PAY_MODE_TO) {
//                computeMinimumGuaranteedPayTO("OPL", "Catégorie A", "Année 0");
//            } else {
//                computeMinimumGuaranteedPayAF();
//            }
//            computeMonthsPay(payMode);
            buildCreditHoursContent(payMode);
        }
    }

    public void refreshCreditHoursAndPV(int payMode) {
        computeMonthsCreditHours(payMode);
        computeMonthsPv(payMode);
        computeMonthsPay(payMode);
        buildCreditHoursContent(payMode);
    }

    /**
     * scann list of event and determine the different months involved
     */
    private ArrayList<MonthActivity> buildMonthsList() {
        boolean firstEvent = true;   // for the first event
        MonthActivity previous;
        ArrayList<MonthActivity> arrayList = new ArrayList<>();

        for (PlanningEvent event : model.getAlEvents()) {
            if (firstEvent) {
                arrayList.add(new MonthActivity(event.getGcBegin()));
                firstEvent = false;
                continue;
            }

            previous = arrayList.get(arrayList.size() - 1);
            if (event.getGcBegin().get(Calendar.MONTH) != previous.getCalStart().get(Calendar.MONTH)) {
                // set end of previous month (a day before new month)
                previous.getCalEnd().setTime(event.getGcBegin().getTime());
                previous.getCalEnd().add(Calendar.DAY_OF_MONTH, -1);
                arrayList.add(new MonthActivity(event.getGcBegin()));
            }

            // update the current monthactivity last day
            arrayList.get(arrayList.size() - 1).getCalEnd().setTime(event.getGcBegin().getTime());

        }
        return arrayList;
    }

    private void computeMonthBlockHours() {
        // loop through all active months
        for (MonthActivity ma : alMonths) {
            //loop through events, deal with corresponding months only
            for (PlanningEvent pe : model.getAlEvents()) {
                if (!isSameMonthAndYear(pe.getGcBegin(), ma.getCalStart())) {
                    continue;
                }
                if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || (pe.getCategory().equals("VOL") && !pe.getSummary().contains("MEP"))) {
                    ma.blockHours += (pe.getGcEnd().getTimeInMillis() - pe.getGcBegin().getTimeInMillis()) / 3600000f;
                }
            }
        }
    }

    private void computeMonthNumberOfDutyLegs() {
        // loop through all active months
        for (MonthActivity ma : alMonths) {
            //loop through events, deal with corresponding months only
            for (PlanningEvent pe : model.getAlEvents()) {
                if (isSameMonthAndYear(pe.getGcBegin(), ma.getCalStart())) {
                    if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || (pe.getCategory().equals("VOL") && !pe.getSummary().contains("MEP"))) {
                        if (pe.getFunction().equals("CDB")) {
                            ma.dutyLegs++;
                        }
                    }
                }
            }
        }
    }

    private float computeTotalBlockHours() {
        float total = 0.0f;
        for (PlanningEvent pe : model.getAlEvents()) {
            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || (pe.getCategory().equals("VOL") && !pe.getSummary().contains("MEP"))) {
                total += (pe.getGcEnd().getTimeInMillis() - pe.getGcBegin().getTimeInMillis()) / 3600000f;
            }
        }
        return total;
    }

    public ArrayList<MonthActivity> getAlMonths() {
        return alMonths;
    }

    public String buildHoursSheetTO() {
        DateFormatSymbols dfs = new DateFormatSymbols();
        final String[] shortMonths = new String[]{"Jan", "Fév", "Mar", "Avr", "Mai", "Jui", "Jul", "Aou", "Sep", "Oct", "Nov", "Déc"};
        dfs.setShortMonths(shortMonths);
        final SimpleDateFormat sdfDay = new SimpleDateFormat("dd", dfs);
        final SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM yyyy", dfs);

        StringBuilder sb = new StringBuilder();
        sb.append("Base: ").append(crewMemberBase);
        sb.append(System.lineSeparator());
        // total hours
        sb.append(model.getPlanningFirstAndLastDatesAsString());
        sb.append(System.lineSeparator());
        sb.append("Heures bloc: ");
        sb.append(Utils.convertDecimalHourstoHoursMinutes(computeTotalBlockHours()));
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());

        // for each month
        for (MonthActivity ma : model.getActivityAnalyser().getAlMonths()) {
            sb.append(sdfMonth.format(ma.getCalStart().getTime()));
            sb.append(" (du ").append(sdfDay.format(ma.getCalStart().getTime()));
            sb.append(" au ").append(sdfDay.format(ma.getCalEnd().getTime()));
            sb.append(")");
            sb.append(System.lineSeparator());
            sb.append("Heures bloc: ");
            sb.append(Utils.convertDecimalHourstoHoursMinutes(ma.blockHours));
            sb.append(System.lineSeparator());
            if (MainApp.userPrefs.autoComputeCreditHours) {
                sb.append("Hcs: ").append(String.format(Locale.ROOT, "%.2f", ma.hcs));
                sb.append(System.lineSeparator());
                sb.append("Hcsi: ").append(String.format(Locale.ROOT, "%.2f", ma.hcsi));
                sb.append(System.lineSeparator());
                sb.append("\u2211H2: ").append(String.format(Locale.ROOT, "%.2f", ma.sumH2));
                sb.append(System.lineSeparator());
                sb.append("\u2211Hcnuit: ").append(String.format(Locale.ROOT, "%.2f", ma.sumHcNuit));
                sb.append(System.lineSeparator());
                sb.append("HCm: ").append(String.format(Locale.ROOT, "%.2f", ma.hcm));
                sb.append(System.lineSeparator());
                sb.append("NJ: ").append(String.valueOf(ma.daysDuty));
                sb.append(System.lineSeparator());
                sb.append("HCgm: ").append(String.format(Locale.ROOT, "%.2f", ma.hcgm));
                sb.append(System.lineSeparator());
                sb.append("HCrm: ").append(String.format(Locale.ROOT, "%.2f", ma.hcrm));
                sb.append(System.lineSeparator());
                sb.append("Seuil SMMG: ").append(String.format(Locale.ROOT, "%.2f", ma.smmgThreshold));
                sb.append(System.lineSeparator());
                sb.append("Seuil Hsup: ").append(String.format(Locale.ROOT, "%.2f", ma.hsupThreshold));
                sb.append(System.lineSeparator());
                sb.append("Hsup: ").append(String.format(Locale.ROOT, "%.2f", ma.hsup));
                sb.append(System.lineSeparator());
                sb.append("Paie (brut, sans primes d'incitation et indemnités diverses): ");
                sb.append(System.lineSeparator());
                sb.append("\tSMMG: ").append(String.format(Locale.ROOT, "%.2f", ma.payMinimum));
                sb.append(System.lineSeparator());
                sb.append("\tPrimes de vol: ").append(String.format(Locale.ROOT, "%.2f", ma.payFlight));
                sb.append(System.lineSeparator());
                sb.append("\tHeures suppl. PN: ").append(String.format(Locale.ROOT, "%.2f", ma.payOvertime));
                sb.append(System.lineSeparator());
                sb.append("\tTotal: ").append(String.format(Locale.ROOT, "%.2f", Math.max(minimumGuaranteedPayTO, ma.payTO)));
                sb.append(System.lineSeparator());

            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String buildSimpleHoursSheetTO() {
        DateFormatSymbols dfs = new DateFormatSymbols();
        final String[] shortMonths = new String[]{"Jan", "Fév", "Mar", "Avr", "Mai", "Jui", "Jul", "Aou", "Sep", "Oct", "Nov", "Déc"};
        dfs.setShortMonths(shortMonths);
        final SimpleDateFormat sdfDay = new SimpleDateFormat("dd", dfs);
        final SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM yyyy", dfs);

        StringBuilder sb = new StringBuilder();
        sb.append("Base: ").append(crewMemberBase);
        sb.append(System.lineSeparator());
        // total hours
        sb.append(model.getPlanningFirstAndLastDatesAsString());
        sb.append(System.lineSeparator());
        sb.append("Heures bloc: ");
        sb.append(Utils.convertDecimalHourstoHoursMinutes(computeTotalBlockHours()));
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());

        // for each month
        for (MonthActivity ma : model.getActivityAnalyser().getAlMonths()) {
            sb.append(sdfMonth.format(ma.getCalStart().getTime()));
            sb.append(" (du ").append(sdfDay.format(ma.getCalStart().getTime()));
            sb.append(" au ").append(sdfDay.format(ma.getCalEnd().getTime()));
            sb.append(")");
            sb.append(System.lineSeparator());
            sb.append("Heures bloc: ");
            sb.append(Utils.convertDecimalHourstoHoursMinutes(ma.blockHours));
            sb.append(System.lineSeparator());
            if (MainApp.userPrefs.autoComputeCreditHours) {
                sb.append("NJ: ").append(String.valueOf(ma.daysDuty));
                sb.append(System.lineSeparator());
                sb.append("HCrm: ").append(String.format(Locale.ROOT, "%.2f", ma.hcrm));
                sb.append(System.lineSeparator());
                sb.append("Seuil SMMG: ").append(String.format(Locale.ROOT, "%.2f", ma.smmgThreshold));
                sb.append(System.lineSeparator());
                sb.append("Seuil Hsup: ").append(String.format(Locale.ROOT, "%.2f", ma.hsupThreshold));
                sb.append(System.lineSeparator());
                sb.append("Hsup: ").append(String.format(Locale.ROOT, "%.2f", ma.hsup));
                sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String buildHoursSheetAF() {
        DateFormatSymbols dfs = new DateFormatSymbols();
        final String[] shortMonths = new String[]{"Jan", "Fév", "Mar", "Avr", "Mai", "Jui", "Jul", "Aou", "Sep", "Oct", "Nov", "Déc"};
        dfs.setShortMonths(shortMonths);
        final SimpleDateFormat sdfDay = new SimpleDateFormat("dd", dfs);
        final SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM yyyy", dfs);

        StringBuilder sb = new StringBuilder();
        sb.append("Base: ").append(crewMemberBase);
        sb.append(System.lineSeparator());
        // total hours
        sb.append(model.getPlanningFirstAndLastDatesAsString());
        sb.append(System.lineSeparator());
        sb.append("Heures bloc: ");
        sb.append(Utils.convertDecimalHourstoHoursMinutes(computeTotalBlockHours()));
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());

        // for each month
        for (MonthActivity ma : model.getActivityAnalyserAF().getAlMonths()) {
            sb.append(sdfMonth.format(ma.getCalStart().getTime()));
            sb.append(" (du ").append(sdfDay.format(ma.getCalStart().getTime()));
            sb.append(" au ").append(sdfDay.format(ma.getCalEnd().getTime()));
            sb.append(")");
            sb.append(System.lineSeparator());
            sb.append("Heures bloc: ");
            sb.append(Utils.convertDecimalHourstoHoursMinutes(ma.blockHours));
            sb.append(System.lineSeparator());
            if (MainApp.userPrefs.autoComputeCreditHours) {
                sb.append("NJ: ").append(String.valueOf(ma.daysDuty));
                sb.append(System.lineSeparator());
                sb.append("Hcs: ").append(String.format(Locale.ROOT, "%.2f", ma.hcs));
                sb.append(System.lineSeparator());
                sb.append("Hcsi: ").append(String.format(Locale.ROOT, "%.2f", ma.hcsi));
                sb.append(System.lineSeparator());
                sb.append("HCd (\u2211H2): ").append(String.format(Locale.ROOT, "%.2f", ma.sumH2));
                sb.append(System.lineSeparator());
                sb.append("HCgm: ").append(String.format(Locale.ROOT, "%.2f", ma.hcgm));
                sb.append(System.lineSeparator());
                sb.append("Total HC: ").append(String.format(Locale.ROOT, "%.2f", ma.hcrm));
                sb.append(System.lineSeparator());
                sb.append("PV/HC: ").append(String.format(Locale.ROOT, "%.2f", ma.pvHcQuotient));
                sb.append(System.lineSeparator());
                sb.append("Seuil HS: ").append(String.format(Locale.ROOT, "%.2f", ma.hsupThreshold));
                sb.append(System.lineSeparator());
                sb.append("Total HS: ").append(String.format(Locale.ROOT, "%.2f", ma.hsup));
                sb.append(System.lineSeparator());
                sb.append("PV rm: ").append(String.format(Locale.ROOT, "%.2f", ma.pvRm));
                sb.append(System.lineSeparator());
                sb.append("PV nuit: ").append(String.format(Locale.ROOT, "%.2f", ma.pvNuit));
                sb.append(System.lineSeparator());
                sb.append("PV CDB: ").append(String.format(Locale.ROOT, "%.2f", ma.pvCommand));
                sb.append(System.lineSeparator());
                sb.append("PV majo heures sup: ").append(String.format(Locale.ROOT, "%.2f", ma.pvHSup));
                sb.append(System.lineSeparator());
                sb.append("PV totales: ").append(String.format(Locale.ROOT, "%.2f", ma.pvTotal));
                sb.append(System.lineSeparator());
                sb.append("Paie (brut, sans primes d'incitation et indemnités diverses): ").append(String.format(Locale.ROOT, "%.2f", Math.max(minimumGuaranteedPayAF, ma.payAF)));
                sb.append(System.lineSeparator());

            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String buildSimpleHoursSheetAF() {
        DateFormatSymbols dfs = new DateFormatSymbols();
        final String[] shortMonths = new String[]{"Jan", "Fév", "Mar", "Avr", "Mai", "Jui", "Jul", "Aou", "Sep", "Oct", "Nov", "Déc"};
        dfs.setShortMonths(shortMonths);
        final SimpleDateFormat sdfDay = new SimpleDateFormat("dd", dfs);
        final SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM yyyy", dfs);

        StringBuilder sb = new StringBuilder();
        sb.append("Base: ").append(crewMemberBase);
        sb.append(System.lineSeparator());
        // total hours
        sb.append(model.getPlanningFirstAndLastDatesAsString());
        sb.append(System.lineSeparator());
        sb.append("Heures bloc: ");
        sb.append(Utils.convertDecimalHourstoHoursMinutes(computeTotalBlockHours()));
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());

        // for each month
        for (MonthActivity ma : model.getActivityAnalyserAF().getAlMonths()) {
            sb.append(sdfMonth.format(ma.getCalStart().getTime()));
            sb.append(" (du ").append(sdfDay.format(ma.getCalStart().getTime()));
            sb.append(" au ").append(sdfDay.format(ma.getCalEnd().getTime()));
            sb.append(")");
            sb.append(System.lineSeparator());
            sb.append("Heures bloc: ");
            sb.append(Utils.convertDecimalHourstoHoursMinutes(ma.blockHours));
            sb.append(System.lineSeparator());
            if (MainApp.userPrefs.autoComputeCreditHours) {
                sb.append("NJ: ").append(String.valueOf(ma.daysDuty));
                sb.append(System.lineSeparator());
                sb.append("Total HC: ").append(String.format(Locale.ROOT, "%.2f", ma.hcrm));
                sb.append(System.lineSeparator());
                sb.append("Total HS: ").append(String.format(Locale.ROOT, "%.2f", ma.hsup));
                sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private void buildRotations() {

        alRotations.clear();

        Rotation rotation = new Rotation();
        DutyPeriod dutyPeriod = new DutyPeriod(crewMemberBase);
        PlanningEvent pe;

        for (int i = 0; i < model.getAlEvents().size(); i++) {
            pe = model.getAlEvents().get(i);

            // case of last event
            if (i == model.getAlEvents().size() - 1) {
                if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                    GregorianCalendar gcLatestArrival;
                    GregorianCalendar gcCurrentDeparture;

                    gcLatestArrival = dutyPeriod.getEvents().get(dutyPeriod.getEvents().size() - 1).getGcEnd();
                    gcCurrentDeparture = pe.getGcBegin();

                    if (gcCurrentDeparture.getTimeInMillis() - gcLatestArrival.getTimeInMillis() < TEN_HOURS) {
                        dutyPeriod.getEvents().add(pe);
                    } else {
                        rotation.getDutyPeriods().add(dutyPeriod);
                        dutyPeriod = new DutyPeriod(crewMemberBase);
                        dutyPeriod.getEvents().add(pe);
                    }
                }
                // close the opened dutyperiod/rotation and add them
                dutyPeriod.getmGcBegin().setTimeInMillis(pe.getGcEnd().getTimeInMillis());
                rotation.getDutyPeriods().add(dutyPeriod);
                // set the start date of the rotation (= date of first event of first dutyperiod)
                rotation.setDateStart(rotation.getDutyPeriods().get(0).getEvents().get(0).getGcBegin().getTimeInMillis());
                // set end date
                DutyPeriod lastDutyPeriod = rotation.getDutyPeriods().get(rotation.getDutyPeriods().size() - 1);
                PlanningEvent lastEvent = lastDutyPeriod.getEvents().get(lastDutyPeriod.getEvents().size() - 1);
                rotation.setDateEnd(lastEvent.getGcEnd().getTimeInMillis());

                alRotations.add(rotation);
                break;
            }

            // case of non flight/deadheading event
            if (!Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                continue;
            }

            // if no element yet, add the event
            if (dutyPeriod.getEvents().isEmpty()) {
                dutyPeriod.getEvents().add(pe);
                continue;
            }
            // compare latest element arrival time with current one's departure time
            // if it's less than minimum rest time (10 hours), it is the same duty period 
            if (!dutyPeriod.getEvents().isEmpty()) {
                GregorianCalendar gcLatestArrival;
                GregorianCalendar gcCurrentDeparture;

                gcLatestArrival = dutyPeriod.getEvents().get(dutyPeriod.getEvents().size() - 1).getGcEnd();
                gcCurrentDeparture = pe.getGcBegin();

                if (gcCurrentDeparture.getTimeInMillis() - gcLatestArrival.getTimeInMillis() < TEN_HOURS) {
                    dutyPeriod.getEvents().add(pe);
                } else {
                    rotation.getDutyPeriods().add(dutyPeriod);
                    dutyPeriod = new DutyPeriod(crewMemberBase);
                    dutyPeriod.getEvents().add(pe);

                    // if new duty starts at base (or at CDG with crew bases at ORY) it's a new rotation
                    if (pe.getAirportOrig().iata.equals(crewMemberBase) || (crewMemberBase.equals("ORY") && pe.getAirportOrig().iata.equals("CDG"))) {
                        // set the date of the rotation (= date of first event of first dutyperiod)
                        rotation.setDateStart(rotation.getDutyPeriods().get(0).getEvents().get(0).getGcBegin().getTimeInMillis());
                        // set end date
                        DutyPeriod lastDutyPeriod = rotation.getDutyPeriods().get(rotation.getDutyPeriods().size() - 1);
                        PlanningEvent lastEvent = lastDutyPeriod.getEvents().get(lastDutyPeriod.getEvents().size() - 1);
                        rotation.setDateEnd(lastEvent.getGcEnd().getTimeInMillis());
                        // add the rotation to the list and create new one
                        alRotations.add(rotation);
                        rotation = new Rotation();
                    }
                }
            }
        }
    }

    private void initTempsReference() {
        alPaidTimes = new ArrayList();
        // Load times directory from file.
        Scanner scanner;
        InputStream is = this.getClass().getResourceAsStream("/data/temps_reference_option_B.csv");
        if (is == null) {
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        scanner = new Scanner(in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] items = line.split(";");
            alPaidTimes.add(new ReferenceTime(items[0], items[1]));
        }
        scanner.close();
    }

    private void computeRotationsCreditHours(int payMode) {
        for (Rotation rotation : alRotations) {
            rotation.computeDays();
            rotation.getDutyPeriods().forEach((dutyPeriod) -> {
                dutyPeriod.computeTimes(alPaidTimes, payMode);
            });
            rotation.computeTimesAndPV();
        }
    }

    private void computeMonthsFlightCreditHours(int payMode) {

        for (MonthActivity ma : alMonths) {
            // TO mode
            if (payMode == PAY_MODE_TO) {
                for (PlanningEvent pe : model.getAlEvents()) {
                    if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || (pe.getCategory().equals("VOL") && !pe.getSummary().contains("MEP"))) {
                        if (isSameMonthAndYear(pe.getGcBegin(), ma.getCalStart())) {
                            ma.sumHcNuit += pe.getHcNuit();
                        }
                    }
                }
            }

            for (Rotation rot : alRotations) {
                GregorianCalendar gcStart = new GregorianCalendar();
                GregorianCalendar gcEnd = new GregorianCalendar();

                gcStart.setTimeInMillis(rot.getDateStart());
                gcEnd.setTimeInMillis(rot.getDateEnd());

                // If start and end of rotation are in considered month, add h2
                if (isSameMonthAndYear(gcStart, ma.getCalStart()) && isSameMonthAndYear(gcEnd, ma.getCalStart())) {
                    ma.sumH2 += rot.h2;
                    if (payMode == PAY_MODE_AF) {
                        ma.sumH2_r += rot.h2_r;
                        ma.pvRm += rot.pvRm;
                        ma.pvNuit += rot.pvNuit;
                    }
                } // if only start OR end is in month, compute prorated hours
                else if (isSameMonthAndYear(gcStart, ma.getCalStart()) || isSameMonthAndYear(gcEnd, ma.getCalStart())) {
                    // mode TO
                    if (payMode == PAY_MODE_TO) {
                        ma.sumH2 += computeProratedHours(rot, ma.getCalStart().get(Calendar.MONTH));
                    } else {
                        //mode AF
                        ma.sumH2_r += computeProratedHours_r(rot, ma.getCalStart().get(Calendar.MONTH));
                        ma.pvRm += rot.pvRm * computeProratedHours_r(rot, ma.getCalStart().get(Calendar.MONTH));
                        ma.pvNuit += rot.pvNuit * computeProratedHours_r(rot, ma.getCalStart().get(Calendar.MONTH));
                    }
                }
            }
        }
    }

    /**
     * Compute prorated counted hours for a rotation which spans over two months
     * pro rata is based on paid times for given month
     *
     * @param rotation
     * @param month
     * @return
     */
    private float computeProratedHours(Rotation rotation, int month) {
        // compute total and month HV(r) for rotation

        float totalHV = 0.0f;
        float monthHV = 0.0f;
        for (DutyPeriod dp : rotation.getDutyPeriods()) {
            totalHV += dp.mSumHV;
            // scan each flight
            for (PlanningEvent pe : dp.getEvents()) {
                if (pe.getGcBegin().get(Calendar.MONTH) == month && pe.getGcEnd().get(Calendar.MONTH) == month) {
                    monthHV += pe.getHv();
                } else if (pe.getGcBegin().get(Calendar.MONTH) == month && pe.getGcEnd().get(Calendar.MONTH) != month) {
                    int h = pe.getGcBegin().get(Calendar.HOUR_OF_DAY);
                    int m = pe.getGcBegin().get(Calendar.MINUTE);
                    int minutes = (24 - h) * 60 - m;
                    monthHV += Utils.convertMillisecondsToDecimalHours(minutes * 60000);
                } else if (pe.getGcBegin().get(Calendar.MONTH) != month && pe.getGcEnd().get(Calendar.MONTH) == month) {
                    int h = pe.getGcEnd().get(Calendar.HOUR_OF_DAY);
                    int m = pe.getGcEnd().get(Calendar.MINUTE);
                    int minutes = h * 60 - m;
                    monthHV += Utils.convertMillisecondsToDecimalHours(minutes * 60000);
                }
            }
//            if (dp.getEvents().get(0).getGcBegin().get(Calendar.MONTH) == month) {
//                monthHVr += dp.mSumHVr;
//            }
        }

        return rotation.h2 * monthHV / totalHV;
    }

    /**
     * Compute prorated paid hours for a rotation which spans over two months
     * pro rata is based on paid times for given month
     *
     * @param rotation
     * @param month
     * @return
     */
    private float computeProratedHours_r(Rotation rotation, int month) {
        // compute total and month HV(r) for rotation

        float totalHV_r = 0.0f;
        float monthHV_r = 0.0f;
        for (DutyPeriod dp : rotation.getDutyPeriods()) {
            totalHV_r += dp.mSumHV_r;
            // scan each flight
            for (PlanningEvent pe : dp.getEvents()) {
                if (pe.getGcBegin().get(Calendar.MONTH) == month && pe.getGcEnd().get(Calendar.MONTH) == month) {
                    monthHV_r += pe.getHv_r();
                } else if (pe.getGcBegin().get(Calendar.MONTH) == month && pe.getGcEnd().get(Calendar.MONTH) != month) {
                    int h = pe.getGcBegin().get(Calendar.HOUR_OF_DAY);
                    int m = pe.getGcBegin().get(Calendar.MINUTE);
                    int minutes = (24 - h) * 60 - m;
                    monthHV_r += Utils.convertMillisecondsToDecimalHours(minutes * 60000);
                } else if (pe.getGcBegin().get(Calendar.MONTH) != month && pe.getGcEnd().get(Calendar.MONTH) == month) {
                    int h = pe.getGcEnd().get(Calendar.HOUR_OF_DAY);
                    int m = pe.getGcEnd().get(Calendar.MINUTE);
                    int minutes = h * 60 - m;
                    monthHV_r += Utils.convertMillisecondsToDecimalHours(minutes * 60000);
                }
            }
//            if (dp.getEvents().get(0).getGcBegin().get(Calendar.MONTH) == month) {
//                monthHVr += dp.mSumHVr;
//            }
        }

        return rotation.h2_r * monthHV_r / totalHV_r;
    }

    private void computeMonthsHcs(int payMode) {

        for (MonthActivity ma : alMonths) {
            int currentDay = 0;

            for (PlanningEvent pe : model.getAlEvents()) {
                // if different month, disregard
                if (!isSameMonthAndYear(pe.getGcBegin(), ma.getCalStart())) {
                    continue;
                }

                // if same day than previous event, disregard...
                if (pe.getGcBegin().get(Calendar.DAY_OF_MONTH) == currentDay) {
                    continue;
                }

                if (Arrays.stream(PlanningEvent.PAID_GROUND_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                    if (pe.getCategory().equals("JDD") || pe.getCategory().equals("JDDC")) {
                        ma.hcs += 5.6f;
                    } else {
                        if (payMode == PAY_MODE_AF && Arrays.stream(PlanningEvent.PAID_SIM_ACTIVITY).anyMatch(pe.getCategory()::equals)) {
                            if (pe.getBlockTime() > 2.0f) {
                                ma.hcs += 5.0f;
                            } else {
                                ma.hcs += 3.0f;
                            }
                        } else {
                            ma.hcs += 4.0f;
                        }
                    }
                }

                if (Arrays.stream(PlanningEvent.PAID_GROUND_ACTIVITIES_INSTRUCTOR).anyMatch(pe.getCategory()::equals)) {
                    ma.hcsi += 4.0f;
                }
                currentDay = pe.getGcBegin().get(Calendar.DAY_OF_MONTH);
            }
        }
    }

    private void computeMonthVacationAndIllnessDays() {
        for (MonthActivity ma : alMonths) {
            int currentDay = 0;

            for (PlanningEvent pe : model.getAlEvents()) {
                // if different month, disregard
                if (!isSameMonthAndYear(pe.getGcBegin(), ma.getCalStart())) {
                    continue;
                }

                // if same day than previous event, disregard...
                if (pe.getGcBegin().get(Calendar.DAY_OF_MONTH) == currentDay) {
                    continue;
                }

                if (Arrays.stream(PlanningEvent.PAID_ILLNESS).anyMatch(pe.getCategory()::equals)) {
                    ma.daysIllness++;
                }
                if (Arrays.stream(PlanningEvent.PAID_VACATION).anyMatch(pe.getCategory()::equals)) {
                    ma.daysVacation++;
                }
                currentDay = pe.getGcBegin().get(Calendar.DAY_OF_MONTH);
            }
        }
    }

    private void computeMonthDutyDays() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));

        for (MonthActivity ma : alMonths) {
            // set a calendar a first day of month, 00h00
            GregorianCalendar cal = new GregorianCalendar(
                    ma.getCalStart().get(Calendar.YEAR),
                    ma.getCalStart().get(Calendar.MONTH),
                    1, 0, 0);

            while (isSameMonthAndYear(cal, ma.getCalStart())) {

                // check if there is a ground activity on the current day
                for (PlanningEvent pe : model.getAlEvents()) {
                    if (Arrays.stream(PlanningEvent.PAID_GROUND_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                        if (sdf.format(cal.getTime()).equals(sdf.format(pe.getGcBegin().getTime())) || sdf.format(cal.getTime()).equals(sdf.format(pe.getGcEnd().getTime()))) {
                            ma.daysDuty++;
                            break;
                        }
                    }
                }

                // check if current day is between (including) rotation start and end
                int calDate = Integer.valueOf(sdf.format(cal.getTimeInMillis()));

                for (Rotation rotation : alRotations) {
                    int start = Integer.valueOf(sdf.format(rotation.getDateStart()));
                    int end = Integer.valueOf(sdf.format(rotation.getDateEnd()));
                    if (calDate >= start && calDate <= end) {
                        ma.daysDuty++;
                        break;
                    }
                }
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
    }

    private void computeMonthsCreditHours(int payMode) {
        for (MonthActivity ma : alMonths) {
            ma.hsupThreshold = MonthActivity.HSUP_DEFAULT - (ma.daysVacation + ma.daysIllness) * MonthActivity.HSUP_VACATION_AND_ILLNESS_SHIFT;

            if (payMode == PAY_MODE_TO) {
                ma.smmgThreshold = MonthActivity.SMMG_DEFAULT - (ma.daysVacation + ma.daysIllness) * MonthActivity.SMMG_VACATION_AND_ILLNESS_SHIFT_PV_TO;
                ma.hcgm = ma.daysDuty * MonthActivity.DUTY_DAY_CREDIT_HOURS_TO;
                ma.hcm = ma.hcs + ma.sumH2 + ma.sumHcNuit;
            } else {
                ma.smmgThreshold = MonthActivity.SMMG_DEFAULT - (ma.daysVacation + ma.daysIllness) * MonthActivity.SMMG_VACATION_AND_ILLNESS_SHIFT_PV_AF;
                ma.hcgm = ma.daysDuty * MonthActivity.DUTY_DAY_CREDIT_HOURS_AF;
                ma.hcm = ma.hcs + ma.sumH2;
            }
            ma.hcrm = Math.max(ma.hcm, ma.hcgm);
            ma.hsup = Math.max(0.0f, ma.hcrm - ma.hsupThreshold);
        }
    }

    private void computeMonthsPv(int payMode) {
        for (MonthActivity ma : alMonths) {
            // mode TO
            if (payMode == PAY_MODE_TO) {
                ma.pvFlight = Math.max(Math.min(ma.hsupThreshold, ma.hcrm) - ma.smmgThreshold, 0.0f);
                ma.pvOvertime = Math.max(ma.hcrm - ma.hsupThreshold, 0.0f);
            } //mode AF
            else {
                // PVrm and PVnight are already calculated within each rotation
                // and added in computeMonthFlightCreditHours
                ma.pvCommand = (crewFunction == UserPrefs.FUNCTION_CPT) ? ma.dutyLegs * 0.2f : 0.0f;
                // compute pv/hc quotient
                ma.pvHcQuotient = (ma.pvRm + ma.pvNuit + ma.pvCommand) / ma.hcrm;
                pvSupRate = (ma.pvRm + ma.pvNuit) * pvei / ma.hcrm;
                // convert in PVEI
                ma.pvHSup = ma.hsup * 0.25f * pvSupRate / pvei;
                // compute total
                ma.pvTotal = ma.pvRm + ma.pvCommand + ma.pvNuit + ma.pvHSup;
            }
        }
    }

    private void computeMonthsPay(int payMode) {

        for (MonthActivity ma : alMonths) {
            // mode TO
            if (payMode == PAY_MODE_TO) {
                ma.payMinimum = fixedPayTO + pvNormTO * ma.smmgThreshold;
                ma.payFlight = ma.pvFlight * pvNormTO;
                ma.payOvertime = ma.pvOvertime * pvSup75hcTO * 1.25f
                        + ma.hsup * fixedPayTO * 1.25f / 75.0f;
                ma.payTO = ma.payMinimum + ma.payFlight + ma.payOvertime;
            } //mode AF
            else {
                ma.payMinimum = fixedPayAF + pvei * ma.smmgThreshold;
                ma.payAF = Math.max(ma.payMinimum,
                        fixedPayAF + pvei * ma.pvTotal
                        + ma.hsup * fixedPayAF * 1.25f / 75.0f);
            }
        }
    }

    public void computeValuePVEI(int function, int classe, int category, boolean atpl) {
        crewFunction = function;
        ArrayList<String> alClasses = buildClassesArray(function);
        for (String s : alClasses) {
            String[] array = s.split(";");
            if (Integer.valueOf(array[0]) == function && Integer.valueOf(array[1]) == classe) {
                float coeff = Float.valueOf(array[2]);
                if (function == UserPrefs.FUNCTION_CPT) {
                    pvei = BASIC_PV * coeff;
                    return;
                } else if (function == UserPrefs.FUNCTION_FO) {
                    if (atpl) {
                        coeff += 0.06f;
                    }
                    switch (category) {
                        case UserPrefs.CATEGORY_A:
                            pvei = BASIC_PV * coeff * 0.70f;
                            break;
                        case UserPrefs.CATEGORY_B:
                            pvei = BASIC_PV * coeff * 0.85f;
                            break;
                        default:
                            pvei = BASIC_PV * coeff;
                            break;
                    }
                    return;
                }
            }
        }
    }

    public void computeFixedPayAF(int function, int echelon) {
        ArrayList<String> alEchelon = buildEchelonsArray();
        // find echelon coefficient
        float coeff = 1.0f;
        for (String s : alEchelon) {
            String[] array = s.split(";");
            if (Integer.valueOf(array[0].substring(8)) == echelon) {
                coeff = Float.valueOf(array[1]);
                break;
            }
        }
        if (function == UserPrefs.FUNCTION_CPT) {
            fixedPayAF = FIXED_PAY_CAPTAIN_ECHELON_1 * coeff;
        } else {
            fixedPayAF = FIXED_PAY_CAPTAIN_ECHELON_1 * 0.665f * coeff;
        }
    }

    public void computeMinimumGuaranteedPayAF() {
        minimumGuaranteedPayAF = fixedPayAF + (pvei * 80);
    }

    public void computeMinimumGuaranteedPayTO(int function, int category, int year) {
        ArrayList<String> payscale = buildPayscaleTOArray();
        for (String s : payscale) {
            String[] array = s.split(";");
            //search year
            if (Integer.valueOf(array[0].substring(6)) == year) {
                if (function == UserPrefs.FUNCTION_CPT) {
                    fixedPayTO = Float.valueOf(array[8]);
                    pvNormTO = Float.valueOf(array[9]);
                    pvSup75hcTO = Float.valueOf(array[10]);
                } else if (function == UserPrefs.FUNCTION_FO) {
                    fixedPayTO = Float.valueOf(array[1]);
                    switch (category) {
                        case UserPrefs.CATEGORY_A:
                            pvNormTO = Float.valueOf(array[2]);
                            pvSup75hcTO = Float.valueOf(array[3]);
                            break;
                        case UserPrefs.CATEGORY_B:
                            pvNormTO = Float.valueOf(array[4]);
                            pvSup75hcTO = Float.valueOf(array[5]);
                            break;
                        case UserPrefs.CATEGORY_C:
                            pvNormTO = Float.valueOf(array[6]);
                            pvSup75hcTO = Float.valueOf(array[7]);
                            break;
                        default:
                            break;
                    }
                }
                break;
            }
        }
        minimumGuaranteedPayTO = fixedPayTO + pvNormTO * 65.0f;
    }

    /*
    for each event, build the tooltip data that will be shown in cell tooltip
    and vCal description
     */
    private void buildCreditHoursContent(int payMode) {
        StringBuilder sb = null;
        String nl = System.lineSeparator();

        for (Rotation rotation : alRotations) {
            for (DutyPeriod dutyPeriod : rotation.getDutyPeriods()) {
                for (PlanningEvent pe : dutyPeriod.getEvents()) {
                    sb = new StringBuilder();

                    // HcNuit
                    if ((pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || (pe.getCategory().equals("VOL") && !pe.getSummary().contains("MEP"))) && payMode == PAY_MODE_TO) {
                        sb.append("Tronçon:").append(nl);
                        sb.append("HVnuit: ").append(String.format(Locale.ROOT, "%.2f", pe.getHvNuit()));
                        sb.append(nl);
                        sb.append("HcNuit: ").append(String.format(Locale.ROOT, "%.2f", pe.getHcNuit()));
                        sb.append(nl);
                    }
                    // Hcv & Hct & H1 & H2
                    if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                        sb.append("Service de vol:").append(nl);
                        sb.append("Hcv: ").append(String.format(Locale.ROOT, "%.2f", dutyPeriod.hcv));
                        sb.append(nl);
                        sb.append("Hct: ").append(String.format(Locale.ROOT, "%.2f", dutyPeriod.hct));
                        sb.append(nl);
                        sb.append("H1: ").append(String.format(Locale.ROOT, "%.2f", dutyPeriod.h1));
                        sb.append(nl);

                        if (payMode == PAY_MODE_AF) {
                            // TSV Nuit
                            sb.append("TSV Nuit: ").append(String.format(Locale.ROOT, "%.2f", dutyPeriod.tsvNuit));
                            sb.append(nl);
                        }
                        sb.append("Rotation:").append(nl);
                        sb.append("\u2211H1: ").append(String.format(Locale.ROOT, "%.2f", rotation.sumH1));
                        sb.append(nl);
                        sb.append("Hca: ").append(String.format(Locale.ROOT, "%.2f", rotation.hca));
                        sb.append(nl);
                        sb.append("H2: ").append(String.format(Locale.ROOT, "%.2f", rotation.h2));
                        sb.append(nl);
                        sb.append(nl);

                    }
                    pe.setTooltipHcContent(sb.toString());
                }
            }
        }
    }

    /**
     * detect the crew member base which is the first departure after off or
     * vacation, or last flight/dead-head event in the planning
     */
    private void detectCrewMemberBase() {
        // search the first rest/vacation event, and then the flight/deadhead
        // immediatly after
        boolean flagAfterOffOrVacation = false;
        for (PlanningEvent pe : model.getAlEvents()) {
            if (Arrays.stream(PlanningEvent.OFF_OR_VACATION).anyMatch(pe.getCategory()::equals)) {
                flagAfterOffOrVacation = true;
                continue;
            }

            if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                if (flagAfterOffOrVacation) {
                    crewMemberBase = pe.getIataOrig();
                    // case of ferry flight 
                    if (crewMemberBase.equals("CDG")) {
                        crewMemberBase = "ORY";
                    }
                    return;
                }
            }
        }

        // if previous technique did not find anything, search for latest 
        // flight/deadhead 
        PlanningEvent e;
        ListIterator<PlanningEvent> li = model.getAlEvents().listIterator(model.getAlEvents().size());
        while (li.hasPrevious()) {
            e = li.previous();
            if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(e.getCategory()::equals)) {
                crewMemberBase = e.getIataDest();
                return;
            }
        }
    }

    public ArrayList<Rotation> getAlRotations() {
        return alRotations;
    }

    /* Construct an array with classes coffeicients depending on function.
    *  Only the lines matching with the crew member function are returned.
    *  String values of function are replaced by their numeric counterparts
     */
    private ArrayList<String> buildClassesArray(int crewFunction) {
        ArrayList<String> list = new ArrayList();
        // Load airport directory from file.
        Scanner scanner;
        InputStream is = this.getClass().getResourceAsStream("/data/classes_af.csv");
        BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            // ignore comments
            if (!line.contains("//") && !line.isBlank()) {
                list.add(line);
            }
        }
        scanner.close();
        return list;
    }

    private ArrayList<String> buildEchelonsArray() {
        ArrayList<String> list = new ArrayList();
        // Load airport directory from file.
        Scanner scanner;
        InputStream is = this.getClass().getResourceAsStream("/data/echelons_af.csv");
        BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            list.add(line);
        }
        scanner.close();
        return list;
    }

    private ArrayList<String> buildPayscaleTOArray() {
        ArrayList<String> list = new ArrayList();
        // Load airport directory from file.
        Scanner scanner;
        InputStream is = this.getClass().getResourceAsStream("/data/payscale_to.csv");
        BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            list.add(line);
        }
        scanner.close();
        return list;
    }

    private boolean isSameMonthAndYear(GregorianCalendar gc1, GregorianCalendar gc2) {
        int month1 = gc1.get(Calendar.MONTH);
        int month2 = gc2.get(Calendar.MONTH);
        int year1 = gc1.get(Calendar.YEAR);
        int year2 = gc2.get(Calendar.YEAR);

        if (month1 == month2 && year1 == year2) {
            return true;
        } else {
            return false;
        }
    }

    private void computeHVandHV_r() {
        // compute sum of paid time (HV100%(r)) and deadheading time for the duty period
        for (PlanningEvent pe : model.getAlEvents()) {
            if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                // get reference time, or blocktime if not found
                float time = findReferenceTime(alPaidTimes, pe.getIataOrig() + "-" + pe.getIataDest());

                // case of no paid time found or flight is "POGO"
                if (time == -1) {
                    if (pe.getIataOrig().equals("CDG") && pe.getIataDest().equals("ORY")) {
                        pe.setHv(1.05f);
                    } else if (pe.getIataOrig().equals("ORY") && pe.getIataDest().equals("CDG")) {
                        pe.setHv(1.05f);
                    } else {
                        pe.setHv(pe.getBlockTime());
                    }
                } else {
                    pe.setHv(time);
                }
                // Air France deal (add 15min to each leg time)
                if (payMode == ActivityAnalyser.PAY_MODE_AF) {
                    // add 15min to each leg time, 35 for legs greater than 2100NM
                    String leg = pe.getIataOrig() + "-" + pe.getIataDest();
                    if (Arrays.stream(legsSup2100NM).anyMatch(leg::equals)) {
                        pe.setHv_r(pe.getHv_r() + 0.58333f);
                    } else {
                        pe.setHv_r(pe.getHv_r() + 0.25f);
                    }
                } else {
                    pe.setHv_r(pe.getHv());
                }
            }
        }
    }

    private float findReferenceTime(ArrayList<ReferenceTime> refTimes, String searchLeg) {
        for (ReferenceTime refTime : refTimes) {
            if (refTime.leg.equals(searchLeg)) {
                return refTime.time;
            }
        }
        return -1;
    }

    private boolean modelHasFlights() {
        for (PlanningEvent pe : model.getAlEvents()) {
            if (Arrays.stream(PlanningEvent.FLIGHT_ACTIVITIES).anyMatch(pe.getCategory()::equals)) {
                return true;
            }
        }
        return false;
    }

    public void setCrewFunction(int crewFunction) {
        this.crewFunction = crewFunction;
    }
}
