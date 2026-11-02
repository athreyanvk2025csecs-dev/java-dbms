package com.cinebook.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Model representing a Payment transaction in CineBook.
 * Corresponds to table 'payments':
 * (payment_id, booking_id, payment_method, payment_status, amount, paid_at).
 *
 * Member 3 Responsibility: Simulated Payment, Cancellation, Refund Management.
 *
 * NOTE: This is a simulation for college demonstration.
 * No real payment gateways, card numbers, UPI PINs, or bank credentials are used.
 */
public class Payment {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String METHOD_CASH = "CASH";
    public static final String METHOD_UPI = "UPI";
    public static final String METHOD_CARD = "CARD";

    private int paymentId;
    private int bookingId;
    private String paymentMethod; // 'CASH', 'UPI', 'CARD'
    private String paymentStatus; // 'SUCCESS', 'REFUNDED', 'FAILED'
    private double amount;
    private LocalDateTime paidAt;

    // Supplementary display fields for receipt formatting
    private String bookingCode;
    private String movieTitle;
    private String customerName;

    public Payment() {
        this.paymentStatus = STATUS_SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    public Payment(int bookingId, String paymentMethod, double amount) {
        this();
        this.bookingId = bookingId;
        this.paymentMethod = (paymentMethod != null) ? paymentMethod.trim().toUpperCase() : METHOD_CASH;
        this.amount = amount;
    }

    public Payment(int bookingId, String paymentMethod, String paymentStatus, double amount) {
        this(bookingId, paymentMethod, amount);
        this.paymentStatus = (paymentStatus != null) ? paymentStatus.trim().toUpperCase() : STATUS_SUCCESS;
    }

    public Payment(int paymentId, int bookingId, String paymentMethod, String paymentStatus, double amount, LocalDateTime paidAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.paymentMethod = (paymentMethod != null) ? paymentMethod.trim().toUpperCase() : METHOD_CASH;
        this.paymentStatus = (paymentStatus != null) ? paymentStatus.trim().toUpperCase() : STATUS_SUCCESS;
        this.amount = amount;
        this.paidAt = paidAt != null ? paidAt : LocalDateTime.now();
    }

    // -----------------------------------------------------------------
    // Receipt Display
    // -----------------------------------------------------------------

    /**
     * Generates a formatted simulated payment receipt.
     * Clearly indicates that this is a simulation.
     *
     * @return formatted receipt string
     */
    public String getReceipt() {
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = paidAt != null ? paidAt.format(dtFormatter) : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================\n");
        sb.append("               SIMULATED PAYMENT RECEIPT               \n");
        sb.append("=======================================================\n");
        sb.append(String.format(Locale.ROOT, " Payment ID    : #%d\n", paymentId));
        sb.append(String.format(Locale.ROOT, " Booking ID    : #%d\n", bookingId));
        if (bookingCode != null && !bookingCode.isEmpty()) {
            sb.append(String.format(Locale.ROOT, " Booking Code  : %s\n", bookingCode));
        }
        if (customerName != null && !customerName.isEmpty()) {
            sb.append(String.format(Locale.ROOT, " Customer      : %s\n", customerName));
        }
        if (movieTitle != null && !movieTitle.isEmpty()) {
            sb.append(String.format(Locale.ROOT, " Movie         : %s\n", movieTitle));
        }
        sb.append(String.format(Locale.ROOT, " Payment Method: %s\n", paymentMethod != null ? paymentMethod : "N/A"));
        sb.append(String.format(Locale.ROOT, " Status        : %s\n", paymentStatus != null ? paymentStatus : "SUCCESS"));
        sb.append(String.format(Locale.ROOT, " Amount Paid   : Rs. %.2f\n", amount));
        sb.append(String.format(Locale.ROOT, " Transaction At: %s\n", formattedDateTime));
        sb.append("-------------------------------------------------------\n");
        sb.append(" [SIMULATION MODE] No bank credentials or money used.  \n");
        sb.append("=======================================================\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Convenience Status Checks
    // -----------------------------------------------------------------

    public boolean isSuccess() {
        return STATUS_SUCCESS.equalsIgnoreCase(paymentStatus);
    }

    public boolean isRefunded() {
        return STATUS_REFUNDED.equalsIgnoreCase(paymentStatus);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equalsIgnoreCase(paymentStatus);
    }

    // -----------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = (paymentMethod != null) ? paymentMethod.trim().toUpperCase() : null;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = (paymentStatus != null) ? paymentStatus.trim().toUpperCase() : null;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return paymentId == payment.paymentId && bookingId == payment.bookingId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, bookingId);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", bookingId=" + bookingId +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", amount=" + amount +
                ", paidAt=" + paidAt +
                '}';
    }
}
