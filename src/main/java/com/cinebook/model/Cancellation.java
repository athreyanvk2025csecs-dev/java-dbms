package com.cinebook.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Model representing a Booking Cancellation and Refund audit record in CineBook.
 * Corresponds to table 'cancellations':
 * (cancellation_id, booking_id, refund_amount, cancelled_at, reason, status).
 *
 * Member 3 Responsibility: Simulated Payment, Cancellation, Refund Management.
 */
public class Cancellation {

    public static final String STATUS_COMPLETED = "COMPLETED";

    private int cancellationId;
    private int bookingId;
    private double refundAmount;
    private LocalDateTime cancelledAt;
    private String reason;
    private String status; // 'COMPLETED'

    // Supplementary display fields for receipt formatting
    private String bookingCode;
    private String movieTitle;
    private String customerName;
    private double originalFinalAmount;

    public Cancellation() {
        this.status = STATUS_COMPLETED;
        this.cancelledAt = LocalDateTime.now();
    }

    public Cancellation(int bookingId, double refundAmount, String reason) {
        this();
        this.bookingId = bookingId;
        this.refundAmount = refundAmount;
        this.reason = reason;
    }

    public Cancellation(int bookingId, double refundAmount, String reason, String status) {
        this(bookingId, refundAmount, reason);
        this.status = (status != null) ? status.trim().toUpperCase() : STATUS_COMPLETED;
    }

    public Cancellation(int cancellationId, int bookingId, double refundAmount, LocalDateTime cancelledAt, String reason, String status) {
        this.cancellationId = cancellationId;
        this.bookingId = bookingId;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt != null ? cancelledAt : LocalDateTime.now();
        this.reason = reason;
        this.status = (status != null) ? status.trim().toUpperCase() : STATUS_COMPLETED;
    }

    // -----------------------------------------------------------------
    // Cancellation & Refund Receipt Display
    // -----------------------------------------------------------------

    /**
     * Formats a detailed cancellation and refund receipt.
     *
     * @return formatted receipt string
     */
    public String getReceipt() {
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = cancelledAt != null ? cancelledAt.format(dtFormatter) : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================\n");
        sb.append("             CANCELLATION & REFUND RECEIPT             \n");
        sb.append("=======================================================\n");
        sb.append(String.format(Locale.ROOT, " Cancellation ID : #%d\n", cancellationId));
        sb.append(String.format(Locale.ROOT, " Booking ID      : #%d\n", bookingId));
        if (bookingCode != null && !bookingCode.isEmpty()) {
            sb.append(String.format(Locale.ROOT, " Booking Code    : %s\n", bookingCode));
        }
        if (customerName != null && !customerName.isEmpty()) {
            sb.append(String.format(Locale.ROOT, " Customer        : %s\n", customerName));
        }
        if (movieTitle != null && !movieTitle.isEmpty()) {
            sb.append(String.format(Locale.ROOT, " Movie           : %s\n", movieTitle));
        }
        if (originalFinalAmount > 0.0) {
            sb.append(String.format(Locale.ROOT, " Original Amount : Rs. %.2f\n", originalFinalAmount));
        }
        sb.append(String.format(Locale.ROOT, " Refund Amount   : Rs. %.2f (80%% of Final Amount)\n", refundAmount));
        sb.append(String.format(Locale.ROOT, " Status          : %s\n", status != null ? status : STATUS_COMPLETED));
        sb.append(String.format(Locale.ROOT, " Cancelled At    : %s\n", formattedDateTime));
        sb.append(String.format(Locale.ROOT, " Reason          : %s\n", reason != null ? reason : "N/A"));
        sb.append("-------------------------------------------------------\n");
        sb.append(" Policy: 80% refund processed; seats restored to pool. \n");
        sb.append("=======================================================\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------

    public int getCancellationId() {
        return cancellationId;
    }

    public void setCancellationId(int cancellationId) {
        this.cancellationId = cancellationId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = (status != null) ? status.trim().toUpperCase() : null;
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

    public double getOriginalFinalAmount() {
        return originalFinalAmount;
    }

    public void setOriginalFinalAmount(double originalFinalAmount) {
        this.originalFinalAmount = originalFinalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cancellation that)) return false;
        return cancellationId == that.cancellationId && bookingId == that.bookingId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cancellationId, bookingId);
    }

    @Override
    public String toString() {
        return "Cancellation{" +
                "cancellationId=" + cancellationId +
                ", bookingId=" + bookingId +
                ", refundAmount=" + refundAmount +
                ", cancelledAt=" + cancelledAt +
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
