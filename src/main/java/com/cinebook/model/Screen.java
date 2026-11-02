package com.cinebook.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a Screen within a Theatre in CineBook.
 * Corresponds to table 'screens' (screen_id, theatre_id, screen_name).
 */
public class Screen {

    private int screenId;
    private int theatreId;
    private String screenName;
    private List<Seat> seats = new ArrayList<>();

    public Screen() {
    }

    public Screen(int theatreId, String screenName) {
        this.theatreId = theatreId;
        this.screenName = screenName;
    }

    public Screen(int screenId, int theatreId, String screenName) {
        this.screenId = screenId;
        this.theatreId = theatreId;
        this.screenName = screenName;
    }

    public int getScreenId() {
        return screenId;
    }

    public void setScreenId(int screenId) {
        this.screenId = screenId;
    }

    public int getTheatreId() {
        return theatreId;
    }

    public void setTheatreId(int theatreId) {
        this.theatreId = theatreId;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = (seats != null) ? seats : new ArrayList<>();
    }

    public void addSeat(Seat seat) {
        if (seat != null) {
            this.seats.add(seat);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Screen screen = (Screen) o;
        return screenId == screen.screenId && theatreId == screen.theatreId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(screenId, theatreId);
    }

    @Override
    public String toString() {
        return String.format("Screen [ID: %d | TheatreID: %d | ScreenName: %s | Total Seats: %d]",
                screenId, theatreId, screenName, seats != null ? seats.size() : 0);
    }
}
