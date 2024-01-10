/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pluszero.rostertogo.model;

/**
 *
 * @author Cyril
 */
public class CrewMember {

    public static final String UNKNOWN_TRIGRAPH = "[inconnu]";
    public static final String UNKNOWN_PILOT = "UKP";
    public static final String UNKNOWN_FLIGHT_ATTENDANT = "UKS";

    public String trigraph;
    public String name;
    public String surname;
    public String phonenumber;
    public String type;

    public CrewMember() {
    }

    public CrewMember(String trigraph, String name, String surname, String type, String phonenumber) {
        this.type = type;
        this.trigraph = trigraph;
        this.name = name;
        this.surname = surname;
        this.phonenumber = phonenumber;
    }
}
