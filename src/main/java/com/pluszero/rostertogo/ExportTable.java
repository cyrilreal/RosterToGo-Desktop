/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo;

import com.pluszero.rostertogo.model.MonthActivity;
import com.pluszero.rostertogo.model.PlanningEvent;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 *
 * @author Cyril
 */
public class ExportTable {

    SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yy");
    SimpleDateFormat sdfHour = new SimpleDateFormat("HH:mm");

    public ExportTable(ArrayList<PlanningEvent> list) {
        generateDataTable(list);
    }

    public ExportTable(ActivityAnalyser analyserTO, ActivityAnalyser analyserAF) {
        generateComparisonTable(analyserTO, analyserAF);
    }

    private void generateDataTable(ArrayList<PlanningEvent> list) {
        StringBuilder sb = new StringBuilder();
        for (PlanningEvent pe : list) {
            sb.append(sdfDate.format(pe.getGcBegin().getTime()));
            sb.append(";");
            sb.append(sdfHour.format(pe.getGcBegin().getTime()));
            sb.append(";");
            sb.append(sdfHour.format(pe.getGcEnd().getTime()));
            sb.append(";");
            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || pe.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
                sb.append(pe.getIataOrig()).append("-").append(pe.getIataDest());
                sb.append(";");
                if (pe.getLagDest() != PlanningEvent.NO_LAG_AVAIL) {
                    sb.append("TU ");
                    if (pe.getLagDest() < 0) {
                        sb.append("-");
                    } else {
                        sb.append("+");
                    }
                    sb.append(pe.getLagDest());
                }
            } else {
                sb.append(pe.getSummary());
            }
            sb.append(System.getProperty("line.separator"));
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

        content.putString(sb.toString().replaceAll("\r\n", "\n"));
        clipboard.setContent(content);
    }

    private void generateComparisonTable(ActivityAnalyser analyserTO, ActivityAnalyser analyserAF) {
        DateFormatSymbols dfs = new DateFormatSymbols();
        final String[] shortMonths = new String[]{"Jan", "Fév", "Mar", "Avr", "Mai", "Jui", "Jul", "Aou", "Sep", "Oct", "Nov", "Déc"};
        dfs.setShortMonths(shortMonths);
        final SimpleDateFormat sdfDay = new SimpleDateFormat("dd", dfs);
        final SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM yyyy", dfs);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < analyserTO.getAlMonths().size(); i++) {
            MonthActivity maTO = analyserTO.getAlMonths().get(i);
            MonthActivity maAF = analyserAF.getAlMonths().get(i);

            sb.append(sdfMonth.format(maTO.getCalStart().getTime()));
            sb.append(";");
            sb.append(Utils.convertDecimalHourstoHoursMinutes(maTO.blockHours));
            sb.append(";");
            // TO
            sb.append(String.format(Locale.ROOT, "%.2f", maTO.hcrm));
            sb.append(";");
            sb.append(String.format(Locale.ROOT, "%.2f", maTO.payTO));
            sb.append(";");
            // AF
            sb.append(String.format(Locale.ROOT, "%.2f", maAF.hcrm));
            sb.append(";");
            sb.append(String.format(Locale.ROOT, "%.2f", maAF.payTO));

            sb.append(System.getProperty("line.separator"));

            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();

            content.putString(sb.toString().replaceAll("\r\n", "\n").replace(".", ","));
            clipboard.setContent(content);
        }
    }
}
