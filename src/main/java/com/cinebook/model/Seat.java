package com.cinebook.model;

import java.util.Objects;

/**
 * Model representing a physical Seat in a Screen.
 * Corresponds to table 'seats' (seat_id, screen_id, seat_code, seat_type).
 */
public class Seat {

    private int seatId;
    private int screenId;
    private String seatCode;
    private String seatType; // 'REGULAR', 'PREMIUM', 'VIP'

    public Seat() {
    }

    public Seat(int screenId, String seatCode, String seatType) {
        this.screenId = screenId;
        this.seatCode = seatCode;
        this.seatType = seatType;
    }

    public Seat(int seatId, int screenId, String seatCode, String seatType) {
        this.seatId = seatId;
        this.screenId = screenId;
        this.seatCode = seatCode;
        this.seatType = seatType;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public int getScreenId() {
        return screenId;
    }

    public void setScreenId(int screenId) {
        this.screenId = screenId;
    }

    public String getSeatCode() {
        return seatCode;
    }

    public void setSeatCode(String seatCode) {
        this.seatCode = seatCode;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return seatId == seat.seatId && screenId == seat.screenId && Objects.equals(seatCode, seat.seatCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatId, screenId, seatCode);
    }

    @Override
    public String toString() {
        return String.format("Seat [ID: %d | Screen: %d | Code: %s | Type: %s]",
                seatId, screenId, seatCode, seatType);
    }
}
