package com.cinebook.model;

import java.util.Objects;

/**
 * Model representing a Show Seat instance (independent seat availability for a show).
 * Corresponds to table 'show_seats' (show_seat_id, show_id, seat_id, status).
 */
public class ShowSeat {

    private int showSeatId;
    private int showId;
    private int seatId;
    private String status; // 'AVAILABLE', 'BOOKED', 'LOCKED'

    // Joined fields from 'seats' table for easy display and pricing
    private String seatCode;
    private String seatType;

    public ShowSeat() {
    }

    public ShowSeat(int showId, int seatId, String status) {
        this.showId = showId;
        this.seatId = seatId;
        this.status = (status == null || status.trim().isEmpty()) ? "AVAILABLE" : status.toUpperCase();
    }

    public ShowSeat(int showSeatId, int showId, int seatId, String status) {
        this.showSeatId = showSeatId;
        this.showId = showId;
        this.seatId = seatId;
        this.status = (status == null || status.trim().isEmpty()) ? "AVAILABLE" : status.toUpperCase();
    }

    public int getShowSeatId() {
        return showSeatId;
    }

    public void setShowSeatId(int showSeatId) {
        this.showSeatId = showSeatId;
    }

    public int getShowId() {
        return showId;
    }

    public void setShowId(int showId) {
        this.showId = showId;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = (status == null || status.trim().isEmpty()) ? "AVAILABLE" : status.toUpperCase();
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

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(this.status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShowSeat showSeat = (ShowSeat) o;
        return showSeatId == showSeat.showSeatId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(showSeatId);
    }

    @Override
    public String toString() {
        return String.format("ShowSeat [ID: %d | ShowID: %d | SeatID: %d | Code: %s | Type: %s | Status: %s]",
                showSeatId, showId, seatId, seatCode, seatType, status);
    }
}
