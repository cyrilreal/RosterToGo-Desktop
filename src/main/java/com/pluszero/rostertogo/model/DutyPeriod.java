/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.model;

import com.pluszero.rostertogo.ActivityAnalyser;
import com.pluszero.rostertogo.Utils;
import static com.pluszero.rostertogo.model.PlanningEvent.COEFF_HCNUIT;
import static com.pluszero.rostertogo.model.PlanningEvent.ONE_DAY_IN_MILLISECONDS;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 *
 * @author Cyril
 */
public class DutyPeriod {

    private static final long ONE_HOUR_MILLISECONDS = 3600000;
    private ArrayList<PlanningEvent> events;
    public GregorianCalendar mGcFirstEventBegin, mGcLastFlightEnd, mGcTsvBegin, mGcTsvEnd;
    public float mBlockTime, mDutyTime, mDeadheadTime; // in decimal hours
    public float tme; // in decimal hours
    public long fdp;   // Flight Duty Period (ORO.FTL.105) in milliseconds
    public float cmt, mSumHV, mSumHV_r, tsv, tsv_r, tsvNuit, hcv, hcv_r, hct, hct_r, h1, h1_r, pvNuit; // in decimal hours, fdp is defined in ORO.FTL.105, tsv is defined in ACE
    private String crewMemberBase;
    public int numberOfFlights;

    public DutyPeriod(String crewMemberBase) {
        this.crewMemberBase = crewMemberBase;
        events = new ArrayList<>();
        mGcFirstEventBegin = new GregorianCalendar();
        mGcFirstEventBegin.setTimeZone(TimeZone.getTimeZone("UTC"));
        mGcLastFlightEnd = new GregorianCalendar();
        mGcLastFlightEnd.setTimeZone(TimeZone.getTimeZone("UTC"));

    }

    public ArrayList<PlanningEvent> getEvents() {
        return events;
    }

    public void computeTimes(ArrayList<ReferenceTime> paidTimes, int payMode) {
        //TODO: May the first bonus computation

        // compute sum of paid time (HV100%(r)) and deadheading time for the duty period
        for (PlanningEvent event : events) {
            if (event.getCategory().equals(PlanningEvent.CAT_FLIGHT) || event.getCategory().equals("VOL")) {
                numberOfFlights++;
                // get block time
                mBlockTime += event.getBlockTime();
                // get reference time, or blocktime if not found
                mSumHV += event.getHv();
                mSumHV_r += event.getHv_r();

            } else if (event.getCategory().equals(PlanningEvent.CAT_DEAD_HEAD)) {
                mDeadheadTime += event.getBlockTime();
            }
        }

        // compute tme
        if (numberOfFlights != 0) {
            tme = Math.max((float) mBlockTime / numberOfFlights, 1);
        } else {
            tme = 1;
        }

        // compute cmt
        cmt = Math.max((float) 70 / (21 * tme + 30), 1);

        // compute hcv & hcv(r)
        hcv = (mSumHV * cmt) + (mDeadheadTime / 2.0f);
        hcv_r = (mSumHV_r * cmt) + (mDeadheadTime / 2.0f);

        // compute hct
        if (numberOfFlights > 0) {
            computeFdpAndTsvTimes();
            // Air France deal
            if (tsv > 10 && payMode == ActivityAnalyser.PAY_MODE_AF) {
                hct = tsv / 1.45f;
            } else {
                hct = tsv / 1.64f;
            }
        }

        // compute H1 & H1(r)
        h1 = Math.max(hcv, hct);
        h1_r = Math.max(hcv_r, hct);
        
        // compute TSV Nuit
        computeTsvNuit();
        pvNuit = tsvNuit * 0.5f;
    }

    public void computeFdpAndTsvTimes() {
        // check if dutyperiod contains a fligth and not only deadheading
        boolean flag = false;
        for (PlanningEvent pe : events) {
            if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || pe.getCategory().equals("VOL")) {
                flag = true;
            }
        }
        // if no flight found, set same time for begin and end
        // otherwise, set begin at first event's begin minus 1h, and end time
        // at last flight'end plus 30min
        if (flag == false) {
            mGcFirstEventBegin.setTimeInMillis(events.get(0).getGcBegin().getTimeInMillis());
            mGcLastFlightEnd.setTimeInMillis(events.get(0).getGcBegin().getTimeInMillis());
            mGcTsvBegin.setTimeInMillis(events.get(0).getGcBegin().getTimeInMillis());
            mGcTsvEnd.setTimeInMillis(events.get(0).getGcBegin().getTimeInMillis());

        } else {
            mGcFirstEventBegin.setTimeInMillis(events.get(0).getGcBegin().getTimeInMillis());

            // find last flight
            PlanningEvent pe;
            for (int i = events.size(); i > 0; i--) {
                pe = events.get(i - 1);
                if (pe.getCategory().equals(PlanningEvent.CAT_FLIGHT) || pe.getCategory().equals("VOL")) {
                    mGcLastFlightEnd.setTimeInMillis(pe.getGcEnd().getTimeInMillis());
                    break;
                }
            }
        }
        // compute FDP (OM-A / ORO.FTL.105 definition)
        if (events.get(0).getIataOrig().equals(crewMemberBase)) {
            fdp = mGcLastFlightEnd.getTimeInMillis() - mGcFirstEventBegin.getTimeInMillis() + ONE_HOUR_MILLISECONDS + ONE_HOUR_MILLISECONDS / 4;
        } else {
            fdp = mGcLastFlightEnd.getTimeInMillis() - mGcFirstEventBegin.getTimeInMillis() + ONE_HOUR_MILLISECONDS;
        }

        // copy FDP time in each flight of the duty period (for display)
        for (PlanningEvent pe : events) {
            pe.setFdpTime(fdp);
        }

        // compute TSV (ACE definition)
        mGcTsvBegin = new GregorianCalendar();
        mGcTsvBegin.setTimeInMillis(mGcFirstEventBegin.getTimeInMillis() - ONE_HOUR_MILLISECONDS);
        mGcTsvEnd = new GregorianCalendar();
        mGcTsvEnd.setTimeInMillis(mGcLastFlightEnd.getTimeInMillis() + ONE_HOUR_MILLISECONDS / 2);

        tsv = Utils.convertMillisecondsToDecimalHours(
                mGcTsvEnd.getTimeInMillis() - mGcTsvBegin.getTimeInMillis());

    }

//    private float findReferenceTime(ArrayList<PaidTime> paidTimes, String leg) {
//        for (PaidTime paidTime : paidTimes) {
//            if (paidTime.idOutbound.equals(leg)) {
//                return paidTime.timeOutbound;
//            }
//            if (paidTime.idInbound.equals(leg)) {
//                return paidTime.timeInbound;
//            }
//        }
//        return -1;
//    }

    public GregorianCalendar getmGcBegin() {
        return mGcFirstEventBegin;
    }

    public void setmGcBegin(GregorianCalendar mGcBegin) {
        this.mGcFirstEventBegin = mGcBegin;
    }

    public GregorianCalendar getmGcEnd() {
        return mGcLastFlightEnd;
    }

    public void setmGcEnd(GregorianCalendar mGcEnd) {
        this.mGcLastFlightEnd = mGcEnd;
    }

    /**
     * compute all TSV hours between 21h and 09h local time
     */
    private void computeTsvNuit() {

        if (numberOfFlights == 0) {
            tsvNuit = 0.0f;
            return;
        }

        long tsvTimeinMillis = 0;

        int offset = TimeZone.getTimeZone("Europe/Paris").getOffset(mGcFirstEventBegin.getTimeInMillis());
        // create calendars set at 9h00 and 21h00 (Paris local time)
        GregorianCalendar gc9loc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gc9loc.set(
                mGcTsvBegin.get(Calendar.YEAR),
                mGcTsvBegin.get(Calendar.MONTH),
                mGcTsvBegin.get(Calendar.DAY_OF_MONTH),
                9,
                0,
                0);
        gc9loc.set(Calendar.MILLISECOND, 0);
        gc9loc.setTimeInMillis(gc9loc.getTimeInMillis() - offset);

        GregorianCalendar gc21loc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gc21loc.set(
                mGcTsvBegin.get(Calendar.YEAR),
                mGcTsvBegin.get(Calendar.MONTH),
                mGcTsvBegin.get(Calendar.DAY_OF_MONTH),
                21,
                0,
                0);
        gc21loc.set(Calendar.MILLISECOND, 0);
        gc21loc.setTimeInMillis(gc21loc.getTimeInMillis() - offset);

        // compute hours before 9h00LT
        if (mGcTsvBegin.getTimeInMillis() < gc9loc.getTimeInMillis()) {
            tsvTimeinMillis = Math.min(
                    gc9loc.getTimeInMillis() - mGcTsvBegin.getTimeInMillis(),
                    mGcTsvEnd.getTimeInMillis() - mGcTsvBegin.getTimeInMillis());
            if (mGcTsvEnd.getTimeInMillis() > gc21loc.getTimeInMillis()) {
                tsvTimeinMillis += mGcTsvEnd.getTimeInMillis() - gc21loc.getTimeInMillis();
            }
            tsvNuit = tsvTimeinMillis / 3600000f;
            return;
        }
        // compute hours after 21h00LT
        if (mGcTsvBegin.getTimeInMillis() > gc9loc.getTimeInMillis()) {
            if (mGcTsvEnd.getTimeInMillis() > gc9loc.getTimeInMillis() + ONE_DAY_IN_MILLISECONDS) {
                tsvTimeinMillis = Math.min(
                        gc9loc.getTimeInMillis() + ONE_DAY_IN_MILLISECONDS - mGcTsvBegin.getTimeInMillis(),
                        gc9loc.getTimeInMillis() + ONE_DAY_IN_MILLISECONDS - gc21loc.getTimeInMillis());
            } else if (mGcTsvEnd.getTimeInMillis() > gc21loc.getTimeInMillis()) {
                tsvTimeinMillis = Math.min(
                        mGcTsvEnd.getTimeInMillis() - mGcTsvBegin.getTimeInMillis(),
                        mGcTsvEnd.getTimeInMillis() - gc21loc.getTimeInMillis());
            }

            tsvNuit = tsvTimeinMillis / 3600000f;
        }
    }
}
