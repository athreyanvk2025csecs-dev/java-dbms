package com.cinebook.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a Theatre in CineBook.
 * Corresponds to table 'theatres' (theatre_id, name, city, address).
 */
public class Theatre {

    private int theatreId;
    private String name;
    private String city;
    private String address;
    private List<Screen> screens = new ArrayList<>();

    public Theatre() {
    }

    public Theatre(String name, String city, String address) {
        this.name = name;
        this.city = city;
        this.address = address;
    }

    public Theatre(int theatreId, String name, String city, String address) {
        this.theatreId = theatreId;
        this.name = name;
        this.city = city;
        this.address = address;
    }

    public int getTheatreId() {
        return theatreId;
    }

    public void setTheatreId(int theatreId) {
        this.theatreId = theatreId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Screen> getScreens() {
        return screens;
    }

    public void setScreens(List<Screen> screens) {
        this.screens = (screens != null) ? screens : new ArrayList<>();
    }

    public void addScreen(Screen screen) {
        if (screen != null) {
            this.screens.add(screen);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Theatre theatre = (Theatre) o;
        return theatreId == theatre.theatreId && Objects.equals(name, theatre.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theatreId, name);
    }

    @Override
    public String toString() {
        return String.format("Theatre [ID: %d | Name: %s | City: %s | Address: %s]",
                theatreId, name, city, address);
    }
}
