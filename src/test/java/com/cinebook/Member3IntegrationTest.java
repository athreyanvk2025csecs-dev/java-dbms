package com.cinebook;

import com.cinebook.dao.BookingDAO;
import com.cinebook.dao.CancellationDAO;
import com.cinebook.dao.PaymentDAO;
import com.cinebook.dao.SeatDAO;
import com.cinebook.database.DBConnection;
import com.cinebook.model.Booking;
import com.cinebook.model.Cancellation;
import com.cinebook.model.Movie;
import com.cinebook.model.Payment;
import com.cinebook.model.Screen;
import com.cinebook.model.Show;
import com.cinebook.model.ShowSeat;
import com.cinebook.model.Theatre;
import com.cinebook.model.User;
import com.cinebook.service.BookingService;
import com.cinebook.service.CancellationService;
import com.cinebook.service.MovieService;
import com.cinebook.service.PaymentService;
import com.cinebook.service.SeatService;
import com.cinebook.service.ShowService;
import com.cinebook.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration Test Suite for CineBook Member 3:
 * Simulated Payment, Cancellation, Refund Management, MySQL Trigger, Stored Procedure, and Integration.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Member3IntegrationTest {

    private static UserService userService;
    private static MovieService movieService;
    private static ShowService showService;
    private static SeatService seatService;
    private static BookingService bookingService;
    private static PaymentService paymentService;
    private static CancellationService cancellationService;

    private static BookingDAO bookingDAO;
    private static SeatDAO seatDAO;
    private static PaymentDAO paymentDAO;
    private static CancellationDAO cancellationDAO;

    private static User testCustomer;
    private static Movie testMovie;
    private static Theatre testTheatre;
    private static Screen testScreen;

    private static int showOffsetMinutes = 0;

    @BeforeAll
    public static void setup() throws SQLException {
        assertTrue(DBConnection.testConnection(), "Database connection must be active");

        userService = new UserService();
        movieService = new MovieService();
        showService = new ShowService();
        seatDAO = new SeatDAO();
        seatService = new SeatService(seatDAO);
        bookingDAO = new BookingDAO();
        bookingService = new BookingService(bookingDAO, seatDAO, seatService);
        paymentDAO = new PaymentDAO();
        paymentService = new PaymentService(paymentDAO, bookingDAO);
        cancellationDAO = new CancellationDAO();
        cancellationService = new CancellationService(cancellationDAO, bookingDAO);

        // Register customer for Member 3 tests
        String email = "m3_tester_" + System.currentTimeMillis() + "@cinebook.com";
        testCustomer = userService.registerCustomer("Member3 Tester", email, "SecretPass123");
        assertNotNull(testCustomer);

        // Provision test movie and theatre
        testMovie = movieService.addMovie(
                "Interstellar: Re-Release",
                "English",
                "Sci-Fi",
                169,
                LocalDate.of(2024, 9, 27)
        );

        testTheatre = movieService.addTheatre(
                "CineBook PVR Gold",
                "Chennai",
                "Velachery Main Road, Chennai, Tamil Nadu"
        );

        testScreen = movieService.addScreen(testTheatre.getTheatreId(), "Gold Screen 1");
        assertNotNull(testScreen);
    }

    private static Show createFreshShow() throws SQLException {
        LocalDate date = LocalDate.now().plusDays(45);
        LocalTime time = LocalTime.of(10, 0).plusMinutes(showOffsetMinutes);
        showOffsetMinutes += 20;
        return showService.scheduleShow(testMovie.getMovieId(), testScreen.getScreenId(), date, time);
    }

    // -----------------------------------------------------------------
    // 1. Successful simulated cash payment
    // -----------------------------------------------------------------
    @Test
    @Order(1)
    public void testSuccessfulSimulatedCashPayment() throws SQLException {
        Show show = createFreshShow();
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("A1", "A2"));
        assertNotNull(booking);

        Payment payment = paymentService.processPayment(booking.getBookingId(), "CASH", true);
        assertNotNull(payment);
        assertTrue(payment.getPaymentId() > 0, "Payment ID must be generated");
        assertEquals("SUCCESS", payment.getPaymentStatus());
        assertEquals("CASH", payment.getPaymentMethod());
        assertEquals(booking.getFinalAmount(), payment.getAmount(), 0.001);
        assertTrue(payment.isSuccess());

        // Verify receipt contains simulation notice
        String receipt = payment.getReceipt();
        assertNotNull(receipt);
        assertTrue(receipt.contains("SIMULATED PAYMENT RECEIPT"));
        assertTrue(receipt.contains("CASH"));
        assertTrue(receipt.contains("SUCCESS"));
        assertTrue(receipt.contains("SIMULATION MODE"));

        // Verify database persistence
        Optional<Payment> dbPayment = paymentDAO.getPaymentByBookingId(booking.getBookingId());
        assertTrue(dbPayment.isPresent());
        assertEquals("SUCCESS", dbPayment.get().getPaymentStatus());
        assertEquals("CASH", dbPayment.get().getPaymentMethod());
    }

    // -----------------------------------------------------------------
    // 2. Successful simulated UPI payment
    // -----------------------------------------------------------------
    @Test
    @Order(2)
    public void testSuccessfulSimulatedUpiPayment() throws SQLException {
        Show show = createFreshShow();
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("C1", "C2"));
        assertNotNull(booking);

        Payment payment = paymentService.processPayment(booking.getBookingId(), "UPI", true);
        assertNotNull(payment);
        assertEquals("SUCCESS", payment.getPaymentStatus());
        assertEquals("UPI", payment.getPaymentMethod());
        assertEquals(booking.getFinalAmount(), payment.getAmount(), 0.001);

        String receipt = payment.getReceipt();
        assertTrue(receipt.contains("UPI"));
        assertTrue(receipt.contains("SUCCESS"));

        Optional<Payment> dbPayment = paymentDAO.getPaymentById(payment.getPaymentId());
        assertTrue(dbPayment.isPresent());
        assertEquals("UPI", dbPayment.get().getPaymentMethod());
    }

    // -----------------------------------------------------------------
    // 3. Successful simulated card payment
    // -----------------------------------------------------------------
    @Test
    @Order(3)
    public void testSuccessfulSimulatedCardPayment() throws SQLException {
        Show show = createFreshShow();
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("E1", "E2"));
        assertNotNull(booking);

        Payment payment = paymentService.processPayment(booking.getBookingId(), "CARD", true);
        assertNotNull(payment);
        assertEquals("SUCCESS", payment.getPaymentStatus());
        assertEquals("CARD", payment.getPaymentMethod());
        assertEquals(booking.getFinalAmount(), payment.getAmount(), 0.001);

        String receipt = payment.getReceipt();
        assertTrue(receipt.contains("CARD"));
        assertTrue(receipt.contains("SUCCESS"));

        Optional<Payment> dbPayment = paymentDAO.getPaymentByBookingId(booking.getBookingId());
        assertTrue(dbPayment.isPresent());
        assertEquals("CARD", dbPayment.get().getPaymentMethod());
    }

    // -----------------------------------------------------------------
    // 4. Payment rejection
    // -----------------------------------------------------------------
    @Test
    @Order(4)
    public void testPaymentRejection() throws SQLException {
        Show show = createFreshShow();
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("B1", "B2"));
        assertNotNull(booking);

        // Customer rejects simulated payment (confirmed = false)
        Payment failedPayment = paymentService.processPayment(booking.getBookingId(), "UPI", false);
        assertNotNull(failedPayment);
        assertEquals("FAILED", failedPayment.getPaymentStatus());
        assertTrue(failedPayment.isFailed());
        assertFalse(failedPayment.isSuccess());

        // Booking must not be marked as paid
        Optional<Payment> paymentOpt = paymentDAO.getPaymentByBookingId(booking.getBookingId());
        assertTrue(paymentOpt.isPresent());
        assertEquals("FAILED", paymentOpt.get().getPaymentStatus());

        // Customer can re-attempt and confirm payment
        Payment successPayment = paymentService.processPayment(booking.getBookingId(), "UPI", true);
        assertNotNull(successPayment);
        assertEquals("SUCCESS", successPayment.getPaymentStatus());
        assertTrue(successPayment.isSuccess());
    }

    // -----------------------------------------------------------------
    // 5. Booking history
    // -----------------------------------------------------------------
    @Test
    @Order(5)
    public void testBookingHistory() throws SQLException {
        List<Booking> userBookings = bookingService.getBookingsByUserId(testCustomer.getUserId());
        assertNotNull(userBookings);
        assertFalse(userBookings.isEmpty(), "Booking history for test customer must have records");

        for (Booking b : userBookings) {
            assertNotNull(b.getBookingCode());
            assertTrue(b.getBookingId() > 0);
            assertNotNull(b.getMovieTitle());
            // Check associated payment
            Optional<Payment> pmt = paymentService.getPaymentByBookingId(b.getBookingId());
            assertNotNull(pmt);
        }
    }

    // -----------------------------------------------------------------
    // 6. Cancellation of a valid booking
    // -----------------------------------------------------------------
    @Test
    @Order(6)
    public void testCancellationOfValidBooking() throws SQLException {
        Show show = createFreshShow();
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("D1", "D2"));
        paymentService.processPayment(booking.getBookingId(), "UPI", true);

        Cancellation cancellation = cancellationService.cancelBooking(booking.getBookingId(), "Customer schedule change");
        assertNotNull(cancellation);
        assertEquals("COMPLETED", cancellation.getStatus());
        assertEquals(booking.getBookingId(), cancellation.getBookingId());
        assertTrue(cancellation.getRefundAmount() > 0);

        // Verify booking status is CANCELLED in DB
        Optional<Booking> updatedBooking = bookingDAO.getBookingById(booking.getBookingId());
        assertTrue(updatedBooking.isPresent());
        assertEquals("CANCELLED", updatedBooking.get().getStatus());

        // Verify payment status is REFUNDED in DB
        Optional<Payment> updatedPayment = paymentDAO.getPaymentByBookingId(booking.getBookingId());
        assertTrue(updatedPayment.isPresent());
        assertEquals("REFUNDED", updatedPayment.get().getPaymentStatus());
        assertTrue(updatedPayment.get().isRefunded());

        // Verify receipt
        String receipt = cancellation.getReceipt();
        assertTrue(receipt.contains("CANCELLATION & REFUND RECEIPT"));
        assertTrue(receipt.contains("COMPLETED"));
        assertTrue(receipt.contains("Customer schedule change"));
    }

    // -----------------------------------------------------------------
    // 7. Cancellation of an invalid booking ID
    // -----------------------------------------------------------------
    @Test
    @Order(7)
    public void testCancellationOfInvalidBookingId() {
        assertThrows(IllegalArgumentException.class, () -> {
            cancellationService.cancelBooking(999999);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cancellationService.cancelBooking(-5);
        });
    }

    // -----------------------------------------------------------------
    // 8. Duplicate cancellation
    // -----------------------------------------------------------------
    @Test
    @Order(8)
    public void testDuplicateCancellation() throws SQLException {
        Show show = createFreshShow();
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("A3", "A4"));
        paymentService.processPayment(booking.getBookingId(), "CASH", true);

        // First cancellation succeeds
        Cancellation firstCancel = cancellationService.cancelBooking(booking.getBookingId());
        assertNotNull(firstCancel);
        assertEquals("COMPLETED", firstCancel.getStatus());

        // Second cancellation attempt must be rejected
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            cancellationService.cancelBooking(booking.getBookingId());
        });

        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    // -----------------------------------------------------------------
    // 9. 80% refund calculation
    // -----------------------------------------------------------------
    @Test
    @Order(9)
    public void testEightyPercentRefundCalculation() throws SQLException {
        Show show = createFreshShow();

        // 1. Without discount: 2 seats @ Premium (Rs.250) = Rs.500.00
        Booking b1 = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("C3", "C4"));
        assertEquals(500.00, b1.getFinalAmount(), 0.001);
        paymentService.processPayment(b1.getBookingId(), "UPI", true);

        Cancellation c1 = cancellationService.cancelBooking(b1.getBookingId());
        // 80% of 500 = 400.00
        assertEquals(400.00, c1.getRefundAmount(), 0.001, "Refund must be exactly 80% of final_amount");

        // 2. With coupon CINE10: 2 seats @ Premium (Rs.500) - 10% (Rs.50) = Rs.450.00 final_amount
        Booking b2 = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("C5", "C6"), "CINE10");
        assertEquals(450.00, b2.getFinalAmount(), 0.001);
        assertEquals(500.00, b2.getTotalAmount(), 0.001);
        paymentService.processPayment(b2.getBookingId(), "UPI", true);

        Cancellation c2 = cancellationService.cancelBooking(b2.getBookingId());
        // 80% of 450 = 360.00 (NOT 80% of 500 = 400)
        assertEquals(360.00, c2.getRefundAmount(), 0.001,
                "Refund must be calculated on final_amount after coupon discount, not original total_amount");
    }

    // -----------------------------------------------------------------
    // 10. Seat availability after cancellation
    // -----------------------------------------------------------------
    @Test
    @Order(10)
    public void testSeatAvailabilityAfterCancellation() throws SQLException {
        Show show = createFreshShow();
        List<String> seats = Arrays.asList("E7", "E8");

        // Initially available
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "E7"));
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "E8"));

        // Book seats
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), seats);
        paymentService.processPayment(booking.getBookingId(), "CARD", true);

        // Now booked
        assertFalse(seatService.isSeatAvailable(show.getShowId(), "E7"));
        assertFalse(seatService.isSeatAvailable(show.getShowId(), "E8"));

        // Cancel booking
        cancellationService.cancelBooking(booking.getBookingId());

        // Seats must be restored to AVAILABLE in show_seats
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "E7"),
                "Seat E7 must be restored to AVAILABLE after cancellation");
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "E8"),
                "Seat E8 must be restored to AVAILABLE after cancellation");
    }

    // -----------------------------------------------------------------
    // 11. Trigger execution (trg_update_seat_status)
    // -----------------------------------------------------------------
    @Test
    @Order(11)
    public void testTriggerExecution() throws SQLException {
        Show show = createFreshShow();

        // Find a specific show_seat that is currently AVAILABLE
        int targetShowSeatId = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT show_seat_id FROM show_seats WHERE show_id = ? AND status = 'AVAILABLE' LIMIT 1")) {
            pstmt.setInt(1, show.getShowId());
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next(), "Must find an available show seat");
                targetShowSeatId = rs.getInt(1);
            }
        }

        // Create a dummy booking record
        Booking dummyBooking = new Booking();
        dummyBooking.setUserId(testCustomer.getUserId());
        dummyBooking.setShowId(show.getShowId());
        dummyBooking.setTotalAmount(150.00);
        dummyBooking.setDiscountAmount(0.00);
        dummyBooking.setFinalAmount(150.00);
        dummyBooking.setStatus("CONFIRMED");

        int dummyBookingId = 0;
        try (Connection conn = DBConnection.getConnection()) {
            dummyBookingId = bookingDAO.insertBooking(conn, dummyBooking);
        }
        assertTrue(dummyBookingId > 0);

        // Manually insert into booking_seats table without updating show_seats
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO booking_seats (booking_id, show_seat_id, seat_price) VALUES (?, ?, ?)")) {
            pstmt.setInt(1, dummyBookingId);
            pstmt.setInt(2, targetShowSeatId);
            pstmt.setDouble(3, 150.00);
            pstmt.executeUpdate();
        }

        // Verify the MySQL trigger 'trg_update_seat_status' automatically updated show_seats to 'BOOKED'
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT status FROM show_seats WHERE show_seat_id = ?")) {
            pstmt.setInt(1, targetShowSeatId);
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                String status = rs.getString("status");
                assertEquals("BOOKED", status,
                        "Trigger trg_update_seat_status must automatically update show_seats status to BOOKED");
            }
        }
    }

    // -----------------------------------------------------------------
    // 12. Stored procedure execution (sp_cancel_booking)
    // -----------------------------------------------------------------
    @Test
    @Order(12)
    public void testStoredProcedureExecution() throws SQLException {
        Show show = createFreshShow();
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("B7", "B8"));
        paymentService.processPayment(booking.getBookingId(), "CASH", true);

        // Call stored procedure directly through DAO
        double refund = cancellationDAO.cancelBookingViaProcedure(booking.getBookingId());
        double expectedRefund = Math.round(booking.getFinalAmount() * 0.80 * 100.0) / 100.0;
        assertEquals(expectedRefund, refund, 0.001, "Stored procedure must return 80% refund");

        // Verify cancellation record was created by stored procedure with status COMPLETED
        Optional<Cancellation> record = cancellationDAO.getCancellationByBookingId(booking.getBookingId());
        assertTrue(record.isPresent());
        assertEquals("COMPLETED", record.get().getStatus());
        assertEquals(expectedRefund, record.get().getRefundAmount(), 0.001);

        // Calling stored procedure on already-cancelled booking must fail
        assertThrows(SQLException.class, () -> {
            cancellationDAO.cancelBookingViaProcedure(booking.getBookingId());
        }, "Calling procedure on already-cancelled booking must raise SQL exception");
    }
}
