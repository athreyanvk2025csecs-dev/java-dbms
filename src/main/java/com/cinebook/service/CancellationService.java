package com.cinebook.service;

import com.cinebook.dao.BookingDAO;
import com.cinebook.dao.CancellationDAO;
import com.cinebook.model.Booking;
import com.cinebook.model.Cancellation;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service managing ticket cancellations, seat releases, and 80% refunds.
 *
 * Member 3 Responsibility:
 * - Cancelling bookings via the MySQL stored procedure 'sp_cancel_booking'.
 * - Releasing booked seats back to AVAILABLE.
 * - Calculating 80% refund strictly based on final_amount (after coupon discount).
 * - Preventing duplicate cancellations and duplicate refund records.
 * - Updating payment status to REFUNDED.
 * - Providing cancellation receipts.
 */
public class CancellationService {

    private final CancellationDAO cancellationDAO;
    private final BookingDAO bookingDAO;

    public CancellationService() {
        this.cancellationDAO = new CancellationDAO();
        this.bookingDAO = new BookingDAO();
    }

    public CancellationService(CancellationDAO cancellationDAO, BookingDAO bookingDAO) {
        this.cancellationDAO = cancellationDAO;
        this.bookingDAO = bookingDAO;
    }

    /**
     * Cancels a booking with default reason via the stored procedure 'sp_cancel_booking'.
     *
     * @param bookingId primary booking identifier
     * @return enriched Cancellation entity
     * @throws IllegalArgumentException on invalid input or non-existent booking
     * @throws IllegalStateException    if booking is already cancelled
     * @throws SQLException             on database error
     */
    public Cancellation cancelBooking(int bookingId) throws SQLException {
        return cancelBooking(bookingId, "Customer requested cancellation (80% refund policy)");
    }

    /**
     * Cancels a booking with custom reason via the stored procedure 'sp_cancel_booking'.
     *
     * Workflow:
     * 1. Pre-validates booking exists.
     * 2. Pre-validates booking is not already cancelled.
     * 3. Executes stored procedure 'sp_cancel_booking':
     *    - Verifies existence and non-cancelled status in DB.
     *    - Calculates 80% of final_amount.
     *    - Releases all seats to 'AVAILABLE'.
     *    - Sets booking status to 'CANCELLED'.
     *    - Sets payment status to 'REFUNDED'.
     *    - Inserts cancellation audit record with status 'COMPLETED'.
     *    - Returns refund amount.
     * 4. Updates reason if custom reason was provided.
     * 5. Returns enriched Cancellation model.
     *
     * @param bookingId primary booking identifier
     * @param reason    cancellation reason description
     * @return enriched Cancellation entity
     * @throws IllegalArgumentException on invalid input or non-existent booking
     * @throws IllegalStateException    if booking is already cancelled
     * @throws SQLException             on database error
     */
    public Cancellation cancelBooking(int bookingId, String reason) throws SQLException {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("Invalid booking ID: " + bookingId);
        }

        // Verify booking existence prior to procedure invocation
        Optional<Booking> bookingOpt = bookingDAO.getBookingById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new IllegalArgumentException("Booking with ID #" + bookingId + " does not exist.");
        }

        Booking booking = bookingOpt.get();
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || cancellationDAO.isBookingCancelled(bookingId)) {
            throw new IllegalStateException("Booking #" + bookingId + " is already cancelled. Duplicate cancellation rejected.");
        }

        // Execute stored procedure 'sp_cancel_booking'
        double refundAmount = cancellationDAO.cancelBookingViaProcedure(bookingId);

        // Update reason if custom reason provided
        if (reason != null && !reason.trim().isEmpty() && !reason.equals("Customer requested cancellation (80% refund policy)")) {
            cancellationDAO.updateCancellationReason(bookingId, reason.trim());
        }

        // Retrieve enriched cancellation record
        Optional<Cancellation> recordOpt = cancellationDAO.getCancellationByBookingId(bookingId);
        if (recordOpt.isPresent()) {
            return recordOpt.get();
        }

        // Fallback fallback construction if retrieval query delay
        Cancellation fallback = new Cancellation(bookingId, refundAmount, reason, Cancellation.STATUS_COMPLETED);
        fallback.setBookingCode(booking.getBookingCode());
        fallback.setOriginalFinalAmount(booking.getFinalAmount());
        fallback.setMovieTitle(booking.getMovieTitle());
        fallback.setCustomerName(booking.getUserName());
        return fallback;
    }

    /**
     * Retrieves cancellation details for a booking.
     *
     * @param bookingId booking identifier
     * @return Optional containing Cancellation entity
     * @throws SQLException on database error
     */
    public Optional<Cancellation> getCancellationByBookingId(int bookingId) throws SQLException {
        return cancellationDAO.getCancellationByBookingId(bookingId);
    }

    /**
     * Retrieves cancellation details by cancellation ID.
     *
     * @param cancellationId cancellation primary key
     * @return Optional containing Cancellation entity
     * @throws SQLException on database error
     */
    public Optional<Cancellation> getCancellationById(int cancellationId) throws SQLException {
        return cancellationDAO.getCancellationById(cancellationId);
    }

    /**
     * Checks if a booking is cancelled.
     *
     * @param bookingId booking identifier
     * @return true if cancelled, false otherwise
     * @throws SQLException on database error
     */
    public boolean isBookingCancelled(int bookingId) throws SQLException {
        return cancellationDAO.isBookingCancelled(bookingId);
    }

    /**
     * Retrieves all cancellation audit records.
     *
     * @return list of cancellations
     * @throws SQLException on database error
     */
    public List<Cancellation> getAllCancellations() throws SQLException {
        return cancellationDAO.getAllCancellations();
    }

    /**
     * Prints cancellation & refund receipt to standard output.
     *
     * @param cancellation Cancellation model
     */
    public void displayReceipt(Cancellation cancellation) {
        if (cancellation == null) {
            System.out.println("No cancellation details available.");
            return;
        }
        System.out.println(cancellation.getReceipt());
    }
}
