/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.model;

import java.util.GregorianCalendar;

/**
 *
 * @author Cyril
 */
public class MonthActivity {

    public static final float DUTY_DAY_CREDIT_HOURS_TO = 5.6f;
    public static final float DUTY_DAY_CREDIT_HOURS_AF = 5.0f;

    public final static float SMMG_DEFAULT = 65.0f;
    public final static float SMMG_DEFAULT_AF = 80.0f;

    public final static float HSUP_DEFAULT = 75.0f;
    public final static float SMMG_VACATION_AND_ILLNESS_SHIFT_PV_TO = 2.17f;
    public final static float SMMG_VACATION_AND_ILLNESS_SHIFT_PV_AF = 2.67f;

    public final static float HSUP_VACATION_AND_ILLNESS_SHIFT = 2.5f;

    private GregorianCalendar calStart;     // earliest event in month
    private GregorianCalendar calEnd;       // latest event in month
    public float blockHours = 0.0f;         // decimal hours
    public float sumH2 = 0.0f;              // decimal hours
    public float sumH2_r = 0.0f;            // decimal hours
    public float sumHcNuit = 0.0f;          // decimal hours
    public float sumTsvNuit = 0.0f;         // decimal hours
    public float hcs = 0.0f;                // decimal hours
    public float hcsi = 0.0f;               // decimal hours
    public float hcm = 0.0f;                // decimal hours
    public float hcgm = 0.0f;               // decimal hours
    public float hcrm = 0.0f;               // decimal hours
    public float smmgThreshold = 0.0f;     // decimal hours
    public float hsupThreshold = 0.0f;     // decimal hours
    public float hsup = 0.0f;               // decimal hours
    public float payMinimum = 0.0f;         // TO mode only
    public float payFlight = 0.0f;          // TO mode only
    public float payOvertime = 0.0f;        // TO mode only
    public float payTO = 0.0f;
    public float payAF = 0.0f;

    public float pvFlight = 0.0f;           // TO mode only
    public float pvOvertime = 0.0f;         // TO mode only
    public float pvRm = 0.0f;
    public float pvNuit = 0.0f;            // AF mode only     
    public float pvCommand = 0.0f;         // AF mode only   
    public float pvHcQuotient = 0.0f;       // AF mode only
    public float pvHSup = 0.0f;            // AF mode only
    public float pvTotal = 0.0f;

    public int daysDuty = 0;
    public int daysVacation = 0;
    public int daysIllness = 0;
    public int dutyLegs = 0;                // AF only, for command bonus

    public MonthActivity() {
    }

    public MonthActivity(GregorianCalendar start) {
        calStart = new GregorianCalendar();
        calStart.setTime(start.getTime());
        calEnd = new GregorianCalendar();
        calEnd.setTime(start.getTime());
    }

    public GregorianCalendar getCalStart() {
        return calStart;
    }

    public GregorianCalendar getCalEnd() {
        return calEnd;
    }
}
