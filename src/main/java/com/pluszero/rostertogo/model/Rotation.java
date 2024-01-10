/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 *
 * @author Cyril
 */
public class Rotation {

    private ArrayList<DutyPeriod> dutyPeriods;
    private int mBlockTime; // in minutes
    private long dateStart; // date/time in ms of first leg
    private long dateEnd; // date/time in ms of last leg's end
    private int days; // number of days of the rotation 
    public float hca, sumH1, sumH1_r, h2, h2_r, pvRm, pvNuit; // in decimal hours
    public float sumHvNuit, sumHcNuit, sumTsvNuit; // in decimal hours

    public Rotation() {
        dutyPeriods = new ArrayList<>();
    }

    /**
     * compute the number of days of the rotation, including days without any
     * activity programmed
     */
    public void computeDays() {
        // get first leg of first duty period
        GregorianCalendar gcFirst = new GregorianCalendar();
        gcFirst.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        gcFirst.setTimeInMillis(dutyPeriods.get(0).getEvents().get(0).getGcBegin().getTimeInMillis());
        gcFirst.add(Calendar.MINUTE, -75);
        // set time at 00h00
        gcFirst.set(Calendar.HOUR_OF_DAY, 0);
        gcFirst.set(Calendar.MINUTE, 0);
        gcFirst.set(Calendar.SECOND, 0);

        // get last leg of last duty period
        GregorianCalendar gcLast = new GregorianCalendar();
        gcLast.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        DutyPeriod dp = dutyPeriods.get(dutyPeriods.size() - 1);
        PlanningEvent pe = dp.getEvents().get(dp.getEvents().size() - 1);
        gcLast.setTimeInMillis(pe.getGcEnd().getTimeInMillis());
        gcLast.add(Calendar.MINUTE, 30);
        gcLast.set(Calendar.HOUR_OF_DAY, 0);
        gcLast.set(Calendar.MINUTE, 0);
        gcLast.set(Calendar.SECOND, 0);

        // add one day to last date, and compare
        gcLast.add(Calendar.DAY_OF_MONTH, 1);
        while (gcFirst.before(gcLast)) {
            days++;
            gcFirst.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    /**
     * compute H2 which is sup of Hca and sum of all H1
     */
    public void computeTimesAndPV() {
        // compute night times
        for (DutyPeriod dp : dutyPeriods) {
            // for AF mode
            sumTsvNuit += dp.tsvNuit;
            for (PlanningEvent event : dp.getEvents()) {
                if (event.getCategory().equals(PlanningEvent.CAT_FLIGHT)) {
                    // for TO
                    sumHvNuit += event.getHvNuit();
                    sumHcNuit += event.getHcNuit();                    
                }
            }
        }
        //compute H1, Hca
        sumH1 = 0.0f;
        sumH1_r = 0.0f;
        hca = 0.0f;
        h2 = 0.0f;
        h2_r = 0.0f;

        hca = days * 4.0f;

        // compute H2 and PV
        for (DutyPeriod dutyPeriod : dutyPeriods) {
            sumH1 += dutyPeriod.h1;
            sumH1_r += dutyPeriod.h1_r;
        }

        h2 = Math.max(hca, sumH1);
        h2_r = Math.max(hca, sumH1_r);
        pvRm = h2_r * 1.13f;
        pvNuit = sumTsvNuit * 0.5f;
    }
    
 

    public ArrayList<DutyPeriod> getDutyPeriods() {
        return dutyPeriods;
    }

    public void setDutyPeriods(ArrayList<DutyPeriod> dutyPeriods) {
        this.dutyPeriods = dutyPeriods;
    }

    public long getDateStart() {
        return dateStart;
    }

    public void setDateStart(long date) {
        this.dateStart = date;
    }

    public long getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(long dateEnd) {
        this.dateEnd = dateEnd;
    }

    public int getmBlockTime() {
        return mBlockTime;
    }

    public void setmBlockTime(int mBlockTime) {
        this.mBlockTime = mBlockTime;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}
