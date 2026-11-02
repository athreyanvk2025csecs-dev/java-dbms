package com.cinebook.service;

import com.cinebook.dao.BookingDAO;
import com.cinebook.dao.PaymentDAO;
import com.cinebook.model.Booking;
import com.cinebook.model.Payment;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Service managing simulated payment processing, receipts, and transaction records.
 *
 * Member 3 Responsibility:
 * - Simulated payments only (CASH, UPI, CARD).
 * - No actual payment APIs, card numbers, UPI PINs, or banking credentials.
 * - Exact payment status values: SUCCESS, REFUNDED, FAILED.
 * - Integration with Member 2's BookingService.
 */
public class PaymentService {

    private final PaymentDAO paymentDAO;
    private final BookingDAO bookingDAO;

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
        this.bookingDAO = new BookingDAO();
    }

    public PaymentService(PaymentDAO paymentDAO, BookingDAO bookingDAO) {
        this.paymentDAO = paymentDAO;
        this.bookingDAO = bookingDAO;
    }

    /**
     * Normalizes payment method input (accepts "1", "2", "3" or "CASH", "UPI", "CARD").
     *
     * @param input user input
     * @return normalized method ("CASH", "UPI", "CARD")
     * @throws IllegalArgumentException if invalid
     */
    public String normalizePaymentMethod(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method cannot be empty.");
        }
        String clean = input.trim().toUpperCase(Locale.ROOT);
        return switch (clean) {
            case "1", "CASH" -> Payment.METHOD_CASH;
            case "2", "UPI" -> Payment.METHOD_UPI;
            case "3", "CARD", "CREDIT_CARD", "DEBIT_CARD" -> Payment.METHOD_CARD;
            default -> throw new IllegalArgumentException("Invalid payment method: '" + input
                    + "'. Supported methods: 1. CASH, 2. UPI, 3. CARD");
        };
    }

    /**
     * Processes a simulated payment for an existing booking.
     *
     * Workflow:
     * 1. Validates that the booking exists and is not cancelled.
     * 2. Checks if an active SUCCESS payment already exists for this booking.
     * 3. Verifies customer confirmation.
     * 4. If confirmed: records payment as SUCCESS and returns enriched Payment.
     * 5. If rejected: records payment as FAILED and does not confirm/pay the booking.
     *
     * @param bookingId     booking identifier
     * @param paymentMethod payment method ("CASH", "UPI", "CARD")
     * @param confirmed     whether customer confirmed payment
     * @return processed Payment entity
     * @throws IllegalArgumentException on invalid input or non-existent booking
     * @throws IllegalStateException    if booking is cancelled or already paid
     * @throws SQLException             on database error
     */
    public Payment processPayment(int bookingId, String paymentMethod, boolean confirmed) throws SQLException {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("Invalid booking ID: " + bookingId);
        }

        // 1. Verify booking exists
        Optional<Booking> bookingOpt = bookingDAO.getBookingById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new IllegalArgumentException("Booking with ID #" + bookingId + " does not exist.");
        }

        Booking booking = bookingOpt.get();
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Cannot process payment for cancelled Booking #" + bookingId + ".");
        }

        // 2. Check for existing successful payment
        Optional<Payment> existingOpt = paymentDAO.getPaymentByBookingId(bookingId);
        if (existingOpt.isPresent() && existingOpt.get().isSuccess()) {
            throw new IllegalStateException("Booking #" + bookingId + " is already paid via "
                    + existingOpt.get().getPaymentMethod() + " (Payment ID #" + existingOpt.get().getPaymentId() + ").");
        }

        String normalizedMethod = normalizePaymentMethod(paymentMethod);
        double payableAmount = booking.getFinalAmount();

        // 3. If rejected by customer
        if (!confirmed) {
            Payment failedPayment = new Payment(bookingId, normalizedMethod, Payment.STATUS_FAILED, payableAmount);
            failedPayment.setPaidAt(LocalDateTime.now());
            failedPayment.setBookingCode(booking.getBookingCode());
            failedPayment.setMovieTitle(booking.getMovieTitle());
            failedPayment.setCustomerName(booking.getUserName());

            int failedId = paymentDAO.insertPayment(failedPayment);
            failedPayment.setPaymentId(failedId);
            return failedPayment;
        }

        // 4. If confirmed by customer: Record simulated payment as SUCCESS
        Payment payment = new Payment(bookingId, normalizedMethod, Payment.STATUS_SUCCESS, payableAmount);
        payment.setPaidAt(LocalDateTime.now());
        payment.setBookingCode(booking.getBookingCode());
        payment.setMovieTitle(booking.getMovieTitle());
        payment.setCustomerName(booking.getUserName());

        int generatedPaymentId = paymentDAO.insertPayment(payment);
        payment.setPaymentId(generatedPaymentId);

        return payment;
    }

    /**
     * Retrieves payment by booking ID.
     *
     * @param bookingId booking identifier
     * @return Optional containing Payment
     * @throws SQLException on database error
     */
    public Optional<Payment> getPaymentByBookingId(int bookingId) throws SQLException {
        return paymentDAO.getPaymentByBookingId(bookingId);
    }

    /**
     * Retrieves payment by payment ID.
     *
     * @param paymentId payment identifier
     * @return Optional containing Payment
     * @throws SQLException on database error
     */
    public Optional<Payment> getPaymentById(int paymentId) throws SQLException {
        return paymentDAO.getPaymentById(paymentId);
    }

    /**
     * Retrieves all payments made by a customer.
     *
     * @param userId customer identifier
     * @return list of payments
     * @throws SQLException on database error
     */
    public List<Payment> getPaymentsByUserId(int userId) throws SQLException {
        return paymentDAO.getPaymentsByUserId(userId);
    }

    /**
     * Prints payment receipt to standard output.
     *
     * @param payment Payment model
     */
    public void displayReceipt(Payment payment) {
        if (payment == null) {
            System.out.println("No payment details available.");
            return;
        }
        System.out.println(payment.getReceipt());
    }
}
