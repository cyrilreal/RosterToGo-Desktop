/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo;

import com.pluszero.rostertogo.model.PlanningEvent;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.scene.control.TableCell;

/**
 *
 * @author Cyril
 */
public class TableCellDate extends TableCell<PlanningEvent, Date> {

    final DateFormatSymbols dfs = new DateFormatSymbols();
    final String[] shortDays = new String[]{"", "Di", "Lu", "Ma", "Me", "Je", "Ve", "Sa"};
    private final String[] shortMonths = new String[]{"Jan", "Fév", "Mar", "Avr", "Mai", "Jui", "Jul", "Aou", "Sep", "Nov", "Déc"};

    SimpleDateFormat sdfDate = new SimpleDateFormat("E dd MMM", dfs);

    TableCellDate() {
        dfs.setShortWeekdays(shortDays);
        dfs.setShortMonths(shortMonths);
    }

    @Override
    public void updateItem(Date item, boolean empty) {
        dfs.setShortWeekdays(shortDays);
        dfs.setShortMonths(shortMonths);
        super.updateItem(item, empty);
        setText(sdfDate.format(item));
    }
}
