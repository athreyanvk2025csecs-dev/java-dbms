package com.cinebook.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Model representing a Movie Ticket Booking in CineBook.
 * Corresponds to table 'bookings':
 * (booking_id, booking_code, user_id, show_id, booking_datetime, total_amount, discount_amount, final_amount, status).
 *
 * Member 2 Responsibility: Booking Management, Pricing, and Booking Summaries.
 */
public class Booking {

    private int bookingId;
    private String bookingCode;
    private int userId;
    private int showId;
    private LocalDateTime bookingDateTime;
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private String status; // 'CONFIRMED', 'CANCELLED', 'PENDING'

    // Supplementary/Enriched fields for console display and booking summaries
    private String movieTitle;
    private String movieLanguage;
    private String theatreName;
    private String theatreCity;
    private String screenName;
    private LocalDate showDate;
    private LocalTime showTime;
    private String userName;
    private String userEmail;

    // Associated seat records
    private List<String> seatCodes = new ArrayList<>();
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    public Booking() {
        this.bookingDateTime = LocalDateTime.now();
        this.status = "CONFIRMED";
        this.totalAmount = 0.0;
        this.discountAmount = 0.0;
        this.finalAmount = 0.0;
    }

    public Booking(int userId, int showId) {
        this();
        this.userId = userId;
        this.showId = showId;
    }

    public Booking(int userId, int showId, double totalAmount, double discountAmount, double finalAmount) {
        this();
        this.userId = userId;
        this.showId = showId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    public Booking(int bookingId, String bookingCode, int userId, int showId,
                   LocalDateTime bookingDateTime, double totalAmount, double discountAmount,
                   double finalAmount, String status) {
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.userId = userId;
        this.showId = showId;
        this.bookingDateTime = bookingDateTime;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.status = status;
    }

    // -----------------------------------------------------------------
    // Overloaded calculateTotal() Methods (Pricing & Coupon Requirement)
    // -----------------------------------------------------------------

    /**
     * Calculates total amount from selected seats using SeatType pricing.
     * Sets discountAmount = 0.0 and finalAmount = totalAmount.
     *
     * @return calculated final amount
     */
    public double calculateTotal() {
        double subtotal = 0.0;
        if (seatCodes != null && !seatCodes.isEmpty()) {
            for (String code : seatCodes) {
                subtotal += SeatType.getPriceForCode(code);
            }
        } else if (bookingSeats != null && !bookingSeats.isEmpty()) {
            for (BookingSeat bs : bookingSeats) {
                subtotal += bs.getSeatPrice();
            }
        }
        this.totalAmount = subtotal;
        this.discountAmount = 0.0;
        this.finalAmount = subtotal;
        return this.finalAmount;
    }

    /**
     * Calculates total amount from selected seats and applies coupon discount if valid.
     * CINE10 gives 10% discount.
     * If the coupon is invalid, explains that the coupon is invalid and applies no discount.
     *
     * @param couponCode promotional coupon code (e.g., "CINE10")
     * @return calculated final amount
     */
    public double calculateTotal(String couponCode) {
        double subtotal = 0.0;
        if (seatCodes != null && !seatCodes.isEmpty()) {
            for (String code : seatCodes) {
                subtotal += SeatType.getPriceForCode(code);
            }
        } else if (bookingSeats != null && !bookingSeats.isEmpty()) {
            for (BookingSeat bs : bookingSeats) {
                subtotal += bs.getSeatPrice();
            }
        }
        this.totalAmount = subtotal;

        if (couponCode != null && !couponCode.trim().isEmpty()) {
            String trimmedCoupon = couponCode.trim().toUpperCase();
            if ("CINE10".equals(trimmedCoupon)) {
                this.discountAmount = Math.round(this.totalAmount * 0.10 * 100.0) / 100.0;
            } else {
                System.out.println("Notice: Coupon code '" + couponCode + "' is invalid. No discount applied.");
                this.discountAmount = 0.0;
            }
        } else {
            this.discountAmount = 0.0;
        }

        this.finalAmount = Math.round((this.totalAmount - this.discountAmount) * 100.0) / 100.0;
        return this.finalAmount;
    }

    // -----------------------------------------------------------------
    // Booking Summary
    // -----------------------------------------------------------------

    /**
     * Generates a formatted console booking summary displaying booking ID,
     * movie title, show time, selected seats, base amount, discount, and final amount.
     *
     * @return formatted booking summary string
     */
    public String getSummary() {
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = bookingDateTime != null ? bookingDateTime.format(dtFormatter) : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================\n");
        sb.append("                  BOOKING CONFIRMATION                  \n");
        sb.append("=======================================================\n");
        sb.append(String.format(Locale.ROOT, " Booking ID    : #%d\n", bookingId));
        sb.append(String.format(Locale.ROOT, " Booking Code  : %s\n", bookingCode != null ? bookingCode : "N/A"));
        sb.append(String.format(Locale.ROOT, " Status        : %s\n", status != null ? status : "CONFIRMED"));
        sb.append(String.format(Locale.ROOT, " Booked At     : %s\n", formattedDateTime));
        if (userName != null) {
            sb.append(String.format(Locale.ROOT, " Customer      : %s\n", userName));
        }
        if (movieTitle != null) {
            sb.append(String.format(Locale.ROOT, " Movie         : %s\n", movieTitle));
        }
        if (theatreName != null || screenName != null) {
            sb.append(String.format(Locale.ROOT, " Theatre / Hall: %s - %s\n",
                    theatreName != null ? theatreName : "N/A",
                    screenName != null ? screenName : "N/A"));
        }
        if (showDate != null || showTime != null) {
            sb.append(String.format(Locale.ROOT, " Show Time     : %s at %s\n",
                    showDate != null ? showDate.toString() : "N/A",
                    showTime != null ? showTime.toString() : "N/A"));
        }
        sb.append(String.format(Locale.ROOT, " Selected Seats: %s\n", (seatCodes != null && !seatCodes.isEmpty()) ? String.join(", ", seatCodes) : "N/A"));
        sb.append("-------------------------------------------------------\n");
        sb.append(String.format(Locale.ROOT, " Total Amount  : Rs. %.2f\n", totalAmount));
        sb.append(String.format(Locale.ROOT, " Discount      : Rs. %.2f\n", discountAmount));
        sb.append(String.format(Locale.ROOT, " Final Amount  : Rs. %.2f\n", finalAmount));
        sb.append("=======================================================\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getShowId() {
        return showId;
    }

    public void setShowId(int showId) {
        this.showId = showId;
    }

    public LocalDateTime getBookingDateTime() {
        return bookingDateTime;
    }

    public void setBookingDateTime(LocalDateTime bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMovieLanguage() {
        return movieLanguage;
    }

    public void setMovieLanguage(String movieLanguage) {
        this.movieLanguage = movieLanguage;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public void setTheatreName(String theatreName) {
        this.theatreName = theatreName;
    }

    public String getTheatreCity() {
        return theatreCity;
    }

    public void setTheatreCity(String theatreCity) {
        this.theatreCity = theatreCity;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public LocalTime getShowTime() {
        return showTime;
    }

    public void setShowTime(LocalTime showTime) {
        this.showTime = showTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public List<String> getSeatCodes() {
        return seatCodes;
    }

    public void setSeatCodes(List<String> seatCodes) {
        this.seatCodes = seatCodes != null ? new ArrayList<>(seatCodes) : new ArrayList<>();
    }

    public List<BookingSeat> getBookingSeats() {
        return bookingSeats;
    }

    public void setBookingSeats(List<BookingSeat> bookingSeats) {
        this.bookingSeats = bookingSeats != null ? new ArrayList<>(bookingSeats) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return bookingId == booking.bookingId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
