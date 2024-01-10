/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo;

import com.pluszero.rostertogo.model.PlanningEvent;
import com.pluszero.rostertogo.model.PlanningModel;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * A class to save current planning in an .ics file with "jours blancs"
 *
 * @author Cyril
 */
public class IcsWriter {

    private String content;

    public IcsWriter(PlanningModel model) {
        buildContent(model);
    }

    public String getContent() {
        return content;
    }

    private void buildContent(PlanningModel model) {
        // dateformatter for UID, STAMP...
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        // calendar for ics time generation id
        GregorianCalendar cal = new GregorianCalendar();
        
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR").append(nl);
        sb.append("VERSION:2.0").append(nl);
        sb.append("METHOD:PUBLISH").append(nl);
        sb.append("PRODID:RosterToGo").append(nl);

        int i = 0;
        for (PlanningEvent pe : model.getAlEvents()) {
            sb.append(nl);
            sb.append("BEGIN:VEVENT").append(nl);
            sb.append("UID:").append(sdf.format(cal.getTime())).append(model.getUserTrigraph()).append(i).append(nl);
            sb.append("DTSTAMP:").append(sdf.format(cal.getTime())).append(nl);
            sb.append("DTSTART;VALUE=DATE-TIME:").append(sdf.format(pe.getGcBegin().getTime())).append(nl);
            sb.append("DTEND;VALUE=DATE-TIME:").append(sdf.format(pe.getGcEnd().getTime())).append(nl);
            sb.append("CATEGORIES:").append(pe.getCategory()).append(nl);
            sb.append("SUMMARY:").append(pe.getSummary()).append(nl);
            sb.append("DESCRIPTION:").append(pe.getDescription().replace("\n", "\\n")).append(nl);
            sb.append("END:VEVENT").append(nl);
            i++;
        }
        
        sb.append(nl);
        sb.append("END:VCALENDAR");
        
        content = sb.toString();
    }

}
