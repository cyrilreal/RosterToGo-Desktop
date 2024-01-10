/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

/**
 *
 * @author Cyril
 */
public class ReferenceTime {

    public String leg;
    public float time;

    public ReferenceTime() {
    }

    public ReferenceTime(String leg, String time) {
        this.leg = leg;
        this.time = convertStringToDecimalHours(time);
    }

    private float convertStringToDecimalHours(String time) {
        LocalTime localTime;
        try {
            localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception e) {
            return -1;
        }
        int hour = localTime.get(ChronoField.CLOCK_HOUR_OF_DAY);
        int minute = localTime.get(ChronoField.MINUTE_OF_HOUR);
        return (float) (hour + minute / 60.0f);
    }
}
