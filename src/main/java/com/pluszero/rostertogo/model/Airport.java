package com.pluszero.rostertogo.model;

public class Airport {
	public String iata = "";
	public String name = "";
	public String icao = "";
        public String city = "";
        public String country = "";
        

    public Airport() {
    }
    
    public Airport(String iata, String name, String icao, String city, String country){
        this.iata = iata;
        this.name = name;
        this.icao = icao;
        this.city = city;
        this.country = country;
    }
}
