/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.gui;

import com.pluszero.rostertogo.MainApp;
import com.pluszero.rostertogo.Utils;
import com.pluszero.rostertogo.model.PlanningEvent;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 *
 * @author Cyril
 */
public class EventCell extends ListCell<PlanningEvent> {
// custom locale

    private final DateFormatSymbols dfs = new DateFormatSymbols();
    private final String[] shortDays = new String[]{"", "Di", "Lu", "Ma", "Me", "Je", "Ve", "Sa"};
    private final String[] shortMonths = new String[]{"jan", "fév", "mar", "avr", "mai", "jun", "jul", "aoû", "sep", "oct", "nov", "déc"};
    private final SimpleDateFormat sdfDate;
    private final SimpleDateFormat sdfHour;
    private final Tooltip tooltip;

    private final String nl = System.lineSeparator();
    private final HBox hbox = new HBox();
    private final Label labDate = new Label();
    private final Label labActivity = new Label();

    public EventCell(String timezone) {
        setPadding(Insets.EMPTY);
        dfs.setShortWeekdays(shortDays);
        dfs.setShortMonths(shortMonths);
        sdfDate = new SimpleDateFormat("E dd MMM", dfs);
        sdfHour = new SimpleDateFormat("HH:mm");
        sdfHour.setTimeZone(TimeZone.getTimeZone(timezone));
        tooltip = new Tooltip();

        labDate.setPadding(new Insets(4));
        labActivity.setPadding(new Insets(4));

        hbox.getChildren().addAll(labDate, labActivity);
    }

    @Override
    public void updateItem(PlanningEvent item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null) {
            setText("");
            return;
        }

        // flight activities
        if (item.getCategory().contains(PlanningEvent.CAT_FLIGHT)) {
            setStyle("-fx-background-color: #87cefa; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains("VOL") && !item.getSummary().contains("MEP")) {
            setStyle("-fx-background-color: #87cefa; -fx-font-family: \"Monospace\";");
        }// dead heading
        else if (item.getCategory().contains(PlanningEvent.CAT_DEAD_HEAD)) {
            setStyle("-fx-background-color: #b0e0e6; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains("VOL") && item.getSummary().contains("MEP")) {
            setStyle("-fx-background-color: #b0e0e6; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_CHECK_IN)) {
            setStyle("-fx-background-color: #b0e0e6; -fx-font-family: \"Monospace\";");
        } // ground activities
        else if (item.getCategory().contains(PlanningEvent.CAT_SYND)) {
            setStyle("-fx-background-color: #deb887; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_UNION)) {
            setStyle("-fx-background-color: #deb887; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SYND_CSE)) {
            setStyle("-fx-background-color: #deb887; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_HOTEL)) {
            setStyle("-fx-background-color: #deb887; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIMU)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_APRS)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_E_APRS)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_MEDICAL)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_LOE1)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_FT1)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_FT2)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_C1)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_C2)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_E1)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_E2)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_LOE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_LVO)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_UPRT)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_ENT1)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_BUR_MIN)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_BUREAU_PNT)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_E_LEARNING)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_E_LEARNING_TECH_LOG)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_GROUND_COURSE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_MDC_COURSE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SECURITY_COURSE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SAFETY_COURSE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_DANGEROUSGOODS_COURSE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_REUNION_COMPAGNIE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_CRM_COURSE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_SIM_LOE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_ENTRETIEN_PRO)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_REUNION_COMPAGNIE)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_REUNION_INSTRUCTEURS)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_REUNION_CONCERTATION)) {
            setStyle("-fx-background-color: #cd853f; -fx-font-family: \"Monospace\";");
            // illness
        } else if (item.getCategory().contains(PlanningEvent.CAT_ILLNESS)) {
            setStyle("-fx-background-color: #ffd700; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_OFF_MALADIE)) {
            setStyle("-fx-background-color: #ffd700; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_FATIGUE)) {
            setStyle("-fx-background-color: #ffd700; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_ENFANT_MALADE)) {
            setStyle("-fx-background-color: #ffd700; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_ACCIDENT_TRAVAIL)) {
            setStyle("-fx-background-color: #ffd700; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_ABSENCE_A_JUSTIFIER)) {
            setStyle("-fx-background-color: #ffb878; -fx-font-family: \"Monospace\";");
        } // days off
        else if (item.getCategory().contains(PlanningEvent.CAT_OFF_DDA)) {
            setStyle("-fx-background-color: #98fb98; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_OFF_GARANTI)) {
            setStyle("-fx-background-color: #98fb98; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_OFF)) {
            setStyle("-fx-background-color: #98fb98; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_OFF_RECUP)) {
            setStyle("-fx-background-color: #98fb98; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_REPOS_POST_COURRIER)) {
            setStyle("-fx-background-color: #98fb98; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_JOUR_INACTIVITE_SPECIAL_ACTIVITE_PARTIELLE)) {
            setStyle("-fx-background-color: #98fb98; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains("REPOS")) {
            setStyle("-fx-background-color: #98fb98; -fx-font-family: \"Monospace\";");
        } // vacations
        else if (item.getCategory().contains(PlanningEvent.CAT_VACATION)) {
            setStyle("-fx-background-color: #00fa9a; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_VACATION_OFF_CAMPAIGN)) {
            setStyle("-fx-background-color: #00fa9a; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_VACATION_BIRTH)) {
            setStyle("-fx-background-color: #00fa9a; -fx-font-family: \"Monospace\";");
        } else if (item.getCategory().contains(PlanningEvent.CAT_CONGES_SUR_BLANC)) {
            setStyle("-fx-background-color: #00fa9a; -fx-font-family: \"Monospace\";");
        }// blanc
        else {
            setStyle("-fx-background-color: #ffffff; -fx-font-family: \"Monospace\";");
        }

        // color weekends in light gray
        if (item.getGcBegin().get(Calendar.DAY_OF_WEEK) == 1 || item.getGcBegin().get(Calendar.DAY_OF_WEEK) == 7) {
            labDate.setStyle("-fx-background-color: #dcdcdc; -fx-font-family: \"Monospace\";");
        } else {
            labDate.setStyle("-fx-background-color: #f5f5f5; -fx-font-family: \"Monospace\";");
        }
        // display date for first event of day
        if (item.isFirstEventOfDay() || item.isDayEvent()) {
            labDate.setText(sdfDate.format(item.getGcBegin().getTime()));
        } else {
            labDate.setText("         ");
        }
        labActivity.setText(generateCellLabel(item));
        setGraphic(hbox);

        if (item.isFirstEventOfDay() || item.isDayEvent()) {
            hbox.setStyle("-fx-border-color: #A9A9A9; -fx-border-width: 1 0 0 0;");
        } else {
            hbox.setStyle("-fx-border-color: #A9A9A9; -fx-border-width: 0 0 0 0;");
        }

        if (!item.isExportable()) {
            setStyle("-fx-background-color: rgba(128, 128, 128, .5); -fx-font-family: \"Monospace\";");
        }
        tooltip.setText(buildToolTip(item));

        if (tooltip.getText() != null && !tooltip.getText().equals("")) {
            setTooltip(tooltip);
        }
    }

    private String generateCellLabel(PlanningEvent item) {
        StringBuilder sb = new StringBuilder();

        if (item.isDayEvent()) {
            sb.append(item.getSummary());
            return sb.toString();
        }

        if (item.getCategory().equals(PlanningEvent.CAT_FLIGHT) || item.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
            sb.append(item.getFltNumber()).append(" ");
            sb.append(sdfHour.format(item.getGcBegin().getTime())).append(" ");
            if (item.getCategory().equals(PlanningEvent.CAT_FLIGHT)) {
                sb.append(item.getIataOrig()).append(" - ");
            } else {
                sb.append(item.getIataOrig()).append(" * ");
            }

            sb.append(item.getIataDest()).append(" ");
            sb.append(sdfHour.format(item.getGcEnd().getTime()));
            if (item.getLagDest() != PlanningEvent.NO_LAG_AVAIL) {
                sb.append(" (TU");
                if (item.getLagDest() < 0) {
                    sb.append(item.getLagDest()).append(")");
                } else {
                    sb.append("+").append(item.getLagDest()).append(")");
                }
            }
            return sb.toString();
        }

        sb.append(sdfHour.format(item.getGcBegin().getTime())).append(" ");
        sb.append(item.getSummary()).append(" ");
        sb.append(sdfHour.format(item.getGcEnd().getTime()));
        return sb.toString();
    }

    private String buildToolTip(PlanningEvent pe) {
        if (pe.isDayEvent()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || pe.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
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

        if (pe.getCrewGnd() != null) {
            sb.append("GND :").append(nl);
            for (String s : pe.getCrewGnd()) {
                sb.append(s).append(nl);
            }
            sb.append(nl);
        }

        if (!pe.getTraining().equals("")) {
            sb.append(pe.getTraining()).append(nl).append(nl);
        }

        if (pe.getCheckInUtc() != null) {
            sb.append(pe.getCheckInUtc()).append(nl);
        }

        if (pe.getCheckInLoc() != null) {
            sb.append(pe.getCheckInLoc()).append(nl);
        }

        if(pe.getsTotalBlock() != null){
            sb.append(pe.getsTotalBlock()).append(nl);
        }
        
        if(pe.getsFlightDutyPeriod()!= null){
            sb.append(pe.getsFlightDutyPeriod()).append(nl);
        }
        
        if(pe.getsDutyTime()!= null){
            sb.append(pe.getsDutyTime()).append(nl);
        }
        
        sb.append(nl);

        if (Arrays.stream(PlanningEvent.PAID_GROUND_ACTIVITIES).anyMatch(pe.getCategory()::equals) || Arrays.stream(PlanningEvent.PAID_GROUND_ACTIVITIES_INSTRUCTOR).anyMatch(pe.getCategory()::equals)) {
            if (!Arrays.stream(PlanningEvent.PAID_SIM_ACTIVITY).anyMatch(pe.getCategory()::equals)) {
                if (!pe.getDescription().equals("")) {
                    sb.append(pe.getDescription().trim()).append(nl).append(nl);
                }
            }
        }

        if (!pe.getHotelData().equals("")) {
            sb.append("Hôtel :").append(nl);
            sb.append(pe.getHotelData()).append(nl).append(nl);
        }

        if (pe.isAcceptedCrewRequest()
                && MainApp.userPrefs.showAcceptedCrewRequest) {
            sb.append("Accepted Crew Request");
        }

        if (pe.getTooltipHcContent() != null) {
            sb.append(pe.getTooltipHcContent());
        }

        return sb.toString();
    }
}
