package com.cinebook.service;

import com.cinebook.dao.BookingDAO;
import com.cinebook.dao.SeatDAO;
import com.cinebook.database.DBConnection;
import com.cinebook.model.Booking;
import com.cinebook.model.BookingSeat;
import com.cinebook.model.SeatType;
import com.cinebook.model.ShowSeat;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service managing ticket booking transactions, pricing calculations, coupons, and booking summaries.
 *
 * Member 2 Responsibility:
 * - Atomic all-or-nothing seat bookings using JDBC transactions.
 * - Validation of all seats before modification (rejects invalid codes such as Z9 or already-booked seats).
 * - Overloaded calculateTotal() methods supporting CINE10 promotional discount.
 * - Pricing derived strictly from SeatType (no hardcoded prices).
 * - Unique BK1001-style booking codes.
 * - Console booking summaries.
 * - Coordination interface for Member 3 cancellation module.
 */
public class BookingService {

    private final BookingDAO bookingDAO;
    private final SeatDAO seatDAO;
    private final SeatService seatService;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.seatDAO = new SeatDAO();
        this.seatService = new SeatService(this.seatDAO);
    }

    public BookingService(BookingDAO bookingDAO, SeatDAO seatDAO, SeatService seatService) {
        this.bookingDAO = bookingDAO;
        this.seatDAO = seatDAO;
        this.seatService = seatService;
    }

    // -----------------------------------------------------------------
    // Overloaded calculateTotal() Implementation (Requirement #7)
    // -----------------------------------------------------------------

    /**
     * Overloaded calculateTotal() for parameterless execution.
     * Calculates base total using default configuration.
     *
     * @return 0.0 default total
     */
    public double calculateTotal() {
        return calculateTotal(Collections.emptyList(), null);
    }

    /**
     * Overloaded calculateTotal(String couponCode).
     *
     * @param couponCode promotional coupon code
     * @return 0.0 default total
     */
    public double calculateTotal(String couponCode) {
        return calculateTotal(Collections.emptyList(), couponCode);
    }

    /**
     * Calculates total amount for a list of seat codes without discount.
     * Derives seat prices strictly from SeatType enum.
     *
     * @param seatCodes list of seat codes (e.g., ["A1", "C4"])
     * @return total base price
     */
    public double calculateTotal(List<String> seatCodes) {
        return calculateTotal(seatCodes, null);
    }

    /**
     * Overloaded calculateTotal(List<String> seatCodes, String couponCode).
     * Applies CINE10 (10% discount) when provided.
     * Explains invalid coupons and applies no discount.
     * Strictly retrieves prices from SeatType enum.
     *
     * @param seatCodes  list of seat codes
     * @param couponCode promotional coupon code (e.g. "CINE10")
     * @return calculated final amount after valid discount
     */
    public double calculateTotal(List<String> seatCodes, String couponCode) {
        if (seatCodes == null || seatCodes.isEmpty()) {
            return 0.0;
        }

        double subtotal = 0.0;
        for (String code : seatCodes) {
            seatService.validateSeatCode(code);
            SeatType type = SeatType.fromSeatCode(code);
            subtotal += type.getPrice();
        }

        double discount = calculateDiscount(subtotal, couponCode);
        return Math.round((subtotal - discount) * 100.0) / 100.0;
    }

    /**
     * Helper to compute discount amount based on coupon code.
     *
     * @param subtotal   base amount
     * @param couponCode coupon code string
     * @return discount amount
     */
    public double calculateDiscount(double subtotal, String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return 0.0;
        }
        String trimmed = couponCode.trim().toUpperCase();
        if ("CINE10".equals(trimmed)) {
            return Math.round(subtotal * 0.10 * 100.0) / 100.0;
        } else {
            System.out.println("Notice: Coupon code '" + couponCode + "' is invalid. No discount applied.");
            return 0.0;
        }
    }

    // -----------------------------------------------------------------
    // Booking Transaction Logic (Requirement #6)
    // -----------------------------------------------------------------

    /**
     * Books one or multiple seats for a show in a single atomic transaction without a coupon.
     *
     * @param userId    customer ID
     * @param showId    show ID
     * @param seatCodes list of seat codes to book
     * @return confirmed Booking entity
     * @throws SQLException on database error or transaction failure
     */
    public Booking bookSeats(int userId, int showId, List<String> seatCodes) throws SQLException {
        return bookSeats(userId, showId, seatCodes, null);
    }

    /**
     * Books one or multiple seats for a show in a single atomic transaction with an optional coupon.
     *
     * Guarantees:
     * 1. Validates every selected seat before making any changes.
     * 2. If even one seat is invalid (e.g., "Z9") or already booked, the entire booking is rejected (all-or-nothing).
     * 3. Prevents duplicate booking of the same seat via row-level locks and transaction rollback.
     * 4. Persists the booking record in 'bookings'.
     * 5. Persists individual seat line items in 'booking_seats'.
     * 6. Marks seats as 'BOOKED' in 'show_seats'.
     * 7. Generates a unique BK1001-style booking code.
     *
     * @param userId     customer identifier
     * @param showId     show identifier
     * @param seatCodes  list of seat codes
     * @param couponCode optional promotional coupon code (e.g., "CINE10")
     * @return confirmed and enriched Booking entity
     * @throws IllegalArgumentException on invalid input or seat syntax
     * @throws IllegalStateException    if any seat is already booked
     * @throws SQLException             on database error
     */
    public Booking bookSeats(int userId, int showId, List<String> seatCodes, String couponCode) throws SQLException {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + userId);
        }
        if (showId <= 0) {
            throw new IllegalArgumentException("Invalid show ID: " + showId);
        }
        if (seatCodes == null || seatCodes.isEmpty()) {
            throw new IllegalArgumentException("No seats selected. Please choose at least one seat.");
        }

        // 1. Syntactic and duplicate validation of all seat codes
        Set<String> normalizedCodesSet = new HashSet<>();
        List<String> normalizedCodes = new ArrayList<>();
        for (String code : seatCodes) {
            if (code == null) {
                throw new IllegalArgumentException("Seat code cannot be null.");
            }
            String upper = code.trim().toUpperCase();
            seatService.validateSeatCode(upper); // Throws IllegalArgumentException for invalid codes like "Z9"

            if (!normalizedCodesSet.add(upper)) {
                throw new IllegalArgumentException("Duplicate seat code selected: '" + upper + "'.");
            }
            normalizedCodes.add(upper);
        }

        // 2. Pricing calculation from SeatType
        double totalAmount = 0.0;
        for (String code : normalizedCodes) {
            totalAmount += SeatType.fromSeatCode(code).getPrice();
        }
        double discountAmount = calculateDiscount(totalAmount, couponCode);
        double finalAmount = Math.round((totalAmount - discountAmount) * 100.0) / 100.0;

        // 3. Atomic JDBC Transaction Execution
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lock the requested show seats in the database (Pessimistic locking)
                List<ShowSeat> lockedSeats = seatDAO.getShowSeatsForUpdate(conn, showId, normalizedCodes);

                Map<String, ShowSeat> seatMap = lockedSeats.stream()
                        .collect(Collectors.toMap(s -> s.getSeatCode().toUpperCase(), s -> s));

                // Verify every requested seat exists for this show
                for (String code : normalizedCodes) {
                    ShowSeat ss = seatMap.get(code);
                    if (ss == null) {
                        throw new IllegalArgumentException("Seat '" + code + "' does not exist for Show #" + showId + ".");
                    }
                    if (!ss.isAvailable()) {
                        throw new IllegalStateException("Seat '" + code + "' is already booked for Show #" + showId + ".");
                    }
                }

                // Create Booking entity
                Booking booking = new Booking();
                booking.setUserId(userId);
                booking.setShowId(showId);
                booking.setBookingDateTime(LocalDateTime.now());
                booking.setTotalAmount(totalAmount);
                booking.setDiscountAmount(discountAmount);
                booking.setFinalAmount(finalAmount);
                booking.setStatus("CONFIRMED");
                booking.setSeatCodes(normalizedCodes);

                // Insert into 'bookings' table (generates BK1001-style code and retrieves generated booking_id)
                int generatedBookingId = bookingDAO.insertBooking(conn, booking);

                // Build line-item records for 'booking_seats' and update 'show_seats'
                List<BookingSeat> bookingSeatsList = new ArrayList<>();
                for (String code : normalizedCodes) {
                    ShowSeat ss = seatMap.get(code);
                    double price = SeatType.fromSeatCode(code).getPrice();

                    BookingSeat bs = new BookingSeat(generatedBookingId, ss.getShowSeatId(), price);
                    bs.setSeatCode(code);
                    bs.setSeatType(ss.getSeatType());
                    bookingSeatsList.add(bs);

                    // Update live show_seats record status to 'BOOKED'
                    boolean updated = seatDAO.updateShowSeatStatus(conn, ss.getShowSeatId(), "BOOKED");
                    if (!updated) {
                        throw new SQLException("Failed to update seat status for seat: " + code);
                    }
                }

                // Batch insert into 'booking_seats'
                bookingDAO.insertBookingSeatsBatch(conn, bookingSeatsList);

                // Commit transaction atomically
                conn.commit();

                // Return fully enriched booking
                return bookingDAO.getBookingById(generatedBookingId).orElse(booking);

            } catch (Exception e) {
                // All-or-nothing rollback on any failure
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // -----------------------------------------------------------------
    // Booking Summary & Display Methods
    // -----------------------------------------------------------------

    /**
     * Generates a formatted console booking summary for a given booking ID.
     *
     * @param bookingId booking identifier
     * @return formatted booking summary string
     * @throws SQLException on database error
     */
    public String getBookingSummary(int bookingId) throws SQLException {
        Optional<Booking> opt = bookingDAO.getBookingById(bookingId);
        if (opt.isEmpty()) {
            return "No booking found with ID: " + bookingId;
        }
        return opt.get().getSummary();
    }

    /**
     * Prints the formatted booking summary directly to standard output.
     *
     * @param bookingId booking identifier
     * @throws SQLException on database error
     */
    public void displayBookingSummary(int bookingId) throws SQLException {
        System.out.println(getBookingSummary(bookingId));
    }

    // -----------------------------------------------------------------
    // Integration Interfaces for Member 3 (Cancellation & Queries)
    // -----------------------------------------------------------------

    /**
     * Retrieves a booking by its primary ID.
     * Used by Member 3 to look up booking details prior to cancellation.
     *
     * @param bookingId booking identifier
     * @return Optional containing Booking
     * @throws SQLException on database error
     */
    public Optional<Booking> getBookingById(int bookingId) throws SQLException {
        return bookingDAO.getBookingById(bookingId);
    }

    /**
     * Retrieves a booking by its unique booking code.
     * Used by Member 3 to find bookings by user-entered code.
     *
     * @param bookingCode booking code (e.g. "BK1001")
     * @return Optional containing Booking
     * @throws SQLException on database error
     */
    public Optional<Booking> getBookingByCode(String bookingCode) throws SQLException {
        return bookingDAO.getBookingByCode(bookingCode);
    }

    /**
     * Retrieves all bookings for a user.
     *
     * @param userId user identifier
     * @return list of bookings
     * @throws SQLException on database error
     */
    public List<Booking> getBookingsByUserId(int userId) throws SQLException {
        return bookingDAO.getBookingsByUserId(userId);
    }

    /**
     * Retrieves all booking seat items for a booking.
     * Used by Member 3 to determine which show seats must be freed upon cancellation.
     *
     * @param bookingId booking identifier
     * @return list of BookingSeat entities
     * @throws SQLException on database error
     */
    public List<BookingSeat> getBookingSeats(int bookingId) throws SQLException {
        return bookingDAO.getBookingSeatsByBookingId(bookingId);
    }
}
