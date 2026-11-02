package com.cinebook.model;

import java.util.Objects;

/**
 * Model representing an individual booked seat line-item.
 * Corresponds to table 'booking_seats' (booking_seat_id, booking_id, show_seat_id, seat_price).
 */
public class BookingSeat {

    private int bookingSeatId;
    private int bookingId;
    private int showSeatId;
    private double seatPrice;

    // Enriched fields from joins for display & auditing
    private String seatCode;
    private String seatType;

    public BookingSeat() {
    }

    public BookingSeat(int bookingId, int showSeatId, double seatPrice) {
        this.bookingId = bookingId;
        this.showSeatId = showSeatId;
        this.seatPrice = seatPrice;
    }

    public BookingSeat(int bookingSeatId, int bookingId, int showSeatId, double seatPrice) {
        this.bookingSeatId = bookingSeatId;
        this.bookingId = bookingId;
        this.showSeatId = showSeatId;
        this.seatPrice = seatPrice;
    }

    public int getBookingSeatId() {
        return bookingSeatId;
    }

    public void setBookingSeatId(int bookingSeatId) {
        this.bookingSeatId = bookingSeatId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getShowSeatId() {
        return showSeatId;
    }

    public void setShowSeatId(int showSeatId) {
        this.showSeatId = showSeatId;
    }

    public double getSeatPrice() {
        return seatPrice;
    }

    public void setSeatPrice(double seatPrice) {
        this.seatPrice = seatPrice;
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
        BookingSeat that = (BookingSeat) o;
        return bookingSeatId == that.bookingSeatId ||
               (bookingId == that.bookingId && showSeatId == that.showSeatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingSeatId, bookingId, showSeatId);
    }

    @Override
    public String toString() {
        return String.format("BookingSeat [ID: %d | BookingID: %d | ShowSeatID: %d | Code: %s | Price: Rs.%.2f]",
                bookingSeatId, bookingId, showSeatId, seatCode != null ? seatCode : "N/A", seatPrice);
    }
}
