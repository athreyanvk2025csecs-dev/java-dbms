package com.cinebook;

import com.cinebook.dao.BookingDAO;
import com.cinebook.dao.SeatDAO;
import com.cinebook.database.DBConnection;
import com.cinebook.model.Bookable;
import com.cinebook.model.Booking;
import com.cinebook.model.BookingSeat;
import com.cinebook.model.Movie;
import com.cinebook.model.Screen;
import com.cinebook.model.SeatType;
import com.cinebook.model.Show;
import com.cinebook.model.ShowSeat;
import com.cinebook.model.Theatre;
import com.cinebook.model.User;
import com.cinebook.service.BookingService;
import com.cinebook.service.MovieService;
import com.cinebook.service.SeatService;
import com.cinebook.service.ShowService;
import com.cinebook.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration Test Suite for CineBook Member 2:
 * Seat Management, Seat Availability, Booking Management, and Booking Summaries.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Member2IntegrationTest {

    private static UserService userService;
    private static MovieService movieService;
    private static ShowService showService;
    private static SeatService seatService;
    private static BookingService bookingService;
    private static SeatDAO seatDAO;
    private static BookingDAO bookingDAO;

    private static User testCustomer;
    private static Movie testMovie;
    private static Theatre testTheatre;
    private static Screen testScreen;

    private static int showTimeMinuteOffset = 0;

    @BeforeAll
    public static void setup() throws SQLException {
        assertTrue(DBConnection.testConnection(), "Database connection must be valid and responsive");

        userService = new UserService();
        movieService = new MovieService();
        showService = new ShowService();
        seatDAO = new SeatDAO();
        seatService = new SeatService(seatDAO);
        bookingDAO = new BookingDAO();
        bookingService = new BookingService(bookingDAO, seatDAO, seatService);

        // Register a dedicated customer for Member 2 testing
        String email = "m2_customer_" + System.currentTimeMillis() + "@cinebook.com";
        testCustomer = userService.registerCustomer("Member2 Tester", email, "SecurePass123");
        assertNotNull(testCustomer);

        // Create movie and venue for testing
        testMovie = movieService.addMovie(
                "Dune: Part Two",
                "English",
                "Sci-Fi / Adventure",
                166,
                LocalDate.of(2024, 3, 1)
        );

        testTheatre = movieService.addTheatre(
                "CineBook Luxe IMAX",
                "Bengaluru",
                "Koramangala 4th Block, Bengaluru, Karnataka"
        );

        testScreen = movieService.addScreen(testTheatre.getTheatreId(), "Audi 1");
        assertNotNull(testScreen);
        assertEquals(40, testScreen.getSeats().size(), "Screen must have 40 physical seats (5x8 grid)");
    }

    private static Show createDedicatedTestShow() throws SQLException {
        LocalDate showDate = LocalDate.now().plusDays(30);
        LocalTime showTime = LocalTime.of(9, 0).plusMinutes(showTimeMinuteOffset);
        showTimeMinuteOffset += 15;
        return showService.scheduleShow(testMovie.getMovieId(), testScreen.getScreenId(), showDate, showTime);
    }

    /**
     * Requirement: Test displaying an empty seat map (all seats 'O').
     */
    @Test
    @Order(1)
    public void testDisplayEmptySeatMap() throws SQLException {
        Show show = createDedicatedTestShow();

        // 1. Two-dimensional array verification (5 rows x 8 cols)
        ShowSeat[][] grid = seatService.getSeatGrid(show.getShowId());
        assertNotNull(grid, "Seat grid must not be null");
        assertEquals(5, grid.length, "Grid must have 5 rows (A-E)");
        assertEquals(8, grid[0].length, "Grid must have 8 columns (1-8)");

        // 2. Character availability grid verification
        char[][] statusGrid = seatService.getSeatStatusGrid(show.getShowId());
        assertEquals(5, statusGrid.length);
        assertEquals(8, statusGrid[0].length);

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 8; c++) {
                assertEquals('O', statusGrid[r][c], "All seats in a newly scheduled show must be 'O' (AVAILABLE)");
            }
        }

        // 3. Formatted console seat map rendering
        String seatMap = seatService.renderSeatMap(show.getShowId());
        assertNotNull(seatMap);
        assertTrue(seatMap.contains("[O]"), "Seat map should display [O] for available seats");
        assertTrue(seatMap.contains("Legend: [O] = Available   [X] = Booked"));
        assertTrue(seatMap.contains("Row E = VIP (Rs.400)"));

        // Verify all 5 rows render [O] for empty show
        for (char row : new char[]{'A', 'B', 'C', 'D', 'E'}) {
            assertTrue(seatMap.contains("  " + row + "   [O]"), "Row " + row + " must start with [O]");
        }

        // Test display method execution
        assertDoesNotThrow(() -> seatService.displaySeatMap(show.getShowId()));
    }

    /**
     * Requirement: Test displaying booked seats ('X' on map).
     */
    @Test
    @Order(2)
    public void testDisplayBookedSeats() throws SQLException {
        Show show = createDedicatedTestShow();

        // Initially all available
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "B4"));

        // Book seat B4
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("B4"));
        assertNotNull(booking);

        // Verify status grid reflects 'X' at B4 (Row B = index 1, Col 4 = index 3)
        char[][] statusGrid = seatService.getSeatStatusGrid(show.getShowId());
        assertEquals('X', statusGrid[1][3], "Seat B4 must be marked 'X' (BOOKED)");
        assertEquals('O', statusGrid[0][0], "Seat A1 must remain 'O' (AVAILABLE)");

        // Verify rendered map contains [X]
        String seatMap = seatService.renderSeatMap(show.getShowId());
        assertTrue(seatMap.contains("[X]"), "Rendered seat map must show [X] for booked seat B4");
        assertFalse(seatService.isSeatAvailable(show.getShowId(), "B4"));
    }

    /**
     * Requirement: Test booking one seat.
     */
    @Test
    @Order(3)
    public void testBookOneSeat() throws SQLException {
        Show show = createDedicatedTestShow();

        // Book single Regular seat A1
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("A1"));

        assertNotNull(booking);
        assertTrue(booking.getBookingId() > 0, "Booking ID must be auto-generated by database");
        assertNotNull(booking.getBookingCode(), "Booking code must be generated");
        assertTrue(booking.getBookingCode().startsWith("BK"), "Booking code must match BK1001-style format");
        assertEquals("CONFIRMED", booking.getStatus());
        assertEquals(150.0, booking.getTotalAmount(), 0.001, "A1 is REGULAR, price must be Rs. 150.00");
        assertEquals(0.0, booking.getDiscountAmount(), 0.001);
        assertEquals(150.0, booking.getFinalAmount(), 0.001);
        assertTrue(booking.getSeatCodes().contains("A1"));

        // Verify database persistence in booking_seats
        List<BookingSeat> seats = bookingDAO.getBookingSeatsByBookingId(booking.getBookingId());
        assertEquals(1, seats.size());
        assertEquals(150.0, seats.get(0).getSeatPrice(), 0.001);
        assertEquals("A1", seats.get(0).getSeatCode());

        // Verify live show_seats status updated to 'BOOKED'
        Optional<ShowSeat> showSeatOpt = seatDAO.getShowSeatByCode(show.getShowId(), "A1");
        assertTrue(showSeatOpt.isPresent());
        assertEquals("BOOKED", showSeatOpt.get().getStatus());
    }

    /**
     * Requirement: Test booking multiple seats in a single transaction.
     */
    @Test
    @Order(4)
    public void testBookMultipleSeats() throws SQLException {
        Show show = createDedicatedTestShow();

        // Book 2 seats in row A (Regular tier: 150 each)
        List<String> seatsToBook = List.of("A2", "A3");
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), seatsToBook);

        assertNotNull(booking);
        assertTrue(booking.getBookingId() > 0);
        assertEquals(300.0, booking.getTotalAmount(), 0.001, "150 + 150 = 300.00");
        assertEquals(300.0, booking.getFinalAmount(), 0.001);
        assertEquals(2, booking.getSeatCodes().size());

        // Verify both show_seats are now BOOKED
        assertFalse(seatService.isSeatAvailable(show.getShowId(), "A2"));
        assertFalse(seatService.isSeatAvailable(show.getShowId(), "A3"));

        // Verify line items in booking_seats
        List<BookingSeat> bsList = bookingDAO.getBookingSeatsByBookingId(booking.getBookingId());
        assertEquals(2, bsList.size());
    }

    /**
     * Requirement: Test rejecting invalid seat code Z9.
     */
    @Test
    @Order(5)
    public void testInvalidSeatZ9() throws SQLException {
        Show show = createDedicatedTestShow();

        // Check validation method directly
        assertFalse(seatService.isValidSeatCode("Z9"), "Z9 must be recognized as invalid seat code");
        assertFalse(seatService.isValidSeatCode("A0"), "A0 must be invalid");
        assertFalse(seatService.isValidSeatCode("A9"), "A9 must be invalid");
        assertFalse(seatService.isValidSeatCode("F1"), "F1 must be invalid");

        // Attempting to book invalid seat Z9 must throw IllegalArgumentException
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("Z9")),
                "Booking Z9 must throw IllegalArgumentException"
        );
        assertTrue(ex.getMessage().contains("Invalid seat code") || ex.getMessage().contains("Z9"));

        // Attempting multi-seat booking containing Z9 must also fail immediately
        assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("A1", "Z9"))
        );
    }

    /**
     * Requirement: Test rejecting already-booked seats.
     */
    @Test
    @Order(6)
    public void testBookingAlreadyBookedSeat() throws SQLException {
        Show show = createDedicatedTestShow();

        // Book seat C1 first
        bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("C1"));
        assertFalse(seatService.isSeatAvailable(show.getShowId(), "C1"));

        // Second booking attempt on C1 must fail
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("C1")),
                "Booking an already-booked seat must throw IllegalStateException"
        );
        assertTrue(ex.getMessage().contains("already booked") || ex.getMessage().contains("C1"));
    }

    /**
     * Requirement: Test all-or-nothing transactional integrity (rollback on conflict).
     */
    @Test
    @Order(7)
    public void testAllOrNothingBooking() throws SQLException {
        Show show = createDedicatedTestShow();

        // Pre-book seat B2
        bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("B2"));
        assertFalse(seatService.isSeatAvailable(show.getShowId(), "B2"));

        // Verify B1 and B3 are currently AVAILABLE
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "B1"));
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "B3"));

        // Attempt booking B1, B2, B3 together (B2 is already taken!)
        assertThrows(
                IllegalStateException.class,
                () -> bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), Arrays.asList("B1", "B2", "B3")),
                "Transaction must fail when one seat is already booked"
        );

        // CRITICAL CHECK: B1 and B3 must REMAIN AVAILABLE (Transaction rolled back completely!)
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "B1"), "B1 must remain AVAILABLE after transaction rollback");
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "B3"), "B3 must remain AVAILABLE after transaction rollback");
    }

    /**
     * Requirement: Test different seat prices:
     * Rows A-B = REGULAR (Rs. 150)
     * Rows C-D = PREMIUM (Rs. 250)
     * Row E    = VIP     (Rs. 400)
     * Prices must never be hardcoded, always retrieved from SeatType.
     */
    @Test
    @Order(8)
    public void testDifferentSeatPrices() throws SQLException {
        // Verify SeatType enum contracts
        assertEquals(150.0, SeatType.REGULAR.getPrice(), 0.001);
        assertEquals(250.0, SeatType.PREMIUM.getPrice(), 0.001);
        assertEquals(400.0, SeatType.VIP.getPrice(), 0.001);

        // Verify row-to-SeatType mappings
        assertEquals(SeatType.REGULAR, SeatType.fromRow('A'));
        assertEquals(SeatType.REGULAR, SeatType.fromRow('B'));
        assertEquals(SeatType.PREMIUM, SeatType.fromRow('C'));
        assertEquals(SeatType.PREMIUM, SeatType.fromRow('D'));
        assertEquals(SeatType.VIP, SeatType.fromRow('E'));

        Show show = createDedicatedTestShow();

        // Book 1 Regular (A5: 150), 1 Premium (C5: 250), 1 VIP (E5: 400)
        List<String> seats = List.of("A5", "C5", "E5");
        double expectedTotal = SeatType.REGULAR.getPrice() + SeatType.PREMIUM.getPrice() + SeatType.VIP.getPrice();
        assertEquals(800.0, expectedTotal, 0.001);

        // Test calculateTotal overload
        double calculated = bookingService.calculateTotal(seats);
        assertEquals(800.0, calculated, 0.001);

        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), seats);
        assertEquals(800.0, booking.getTotalAmount(), 0.001);
        assertEquals(800.0, booking.getFinalAmount(), 0.001);

        // Verify persisted seat prices in booking_seats
        List<BookingSeat> lineItems = bookingDAO.getBookingSeatsByBookingId(booking.getBookingId());
        assertEquals(3, lineItems.size());

        BookingSeat regularItem = lineItems.stream().filter(bs -> "A5".equals(bs.getSeatCode())).findFirst().orElseThrow();
        assertEquals(150.0, regularItem.getSeatPrice(), 0.001);

        BookingSeat premiumItem = lineItems.stream().filter(bs -> "C5".equals(bs.getSeatCode())).findFirst().orElseThrow();
        assertEquals(250.0, premiumItem.getSeatPrice(), 0.001);

        BookingSeat vipItem = lineItems.stream().filter(bs -> "E5".equals(bs.getSeatCode())).findFirst().orElseThrow();
        assertEquals(400.0, vipItem.getSeatPrice(), 0.001);
    }

    /**
     * Requirement: Test CINE10 coupon (10% discount) and invalid coupon handling.
     */
    @Test
    @Order(9)
    public void testCine10Coupon() throws SQLException {
        Show show = createDedicatedTestShow();

        // Book seats A6 (150) + C6 (250) + E6 (400) = Rs. 800.00
        List<String> seats = List.of("A6", "C6", "E6");

        // 1. Valid coupon CINE10: 10% discount of 800 = 80, final = 720
        double totalWithCoupon = bookingService.calculateTotal(seats, "CINE10");
        assertEquals(720.0, totalWithCoupon, 0.001);

        Booking bookingWithDiscount = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), seats, "CINE10");
        assertEquals(800.0, bookingWithDiscount.getTotalAmount(), 0.001);
        assertEquals(80.0, bookingWithDiscount.getDiscountAmount(), 0.001);
        assertEquals(720.0, bookingWithDiscount.getFinalAmount(), 0.001);

        // 2. Invalid coupon test on another show
        Show show2 = createDedicatedTestShow();
        List<String> seats2 = List.of("A6", "C6", "E6");

        double totalWithInvalidCoupon = bookingService.calculateTotal(seats2, "DISCOUNT50");
        assertEquals(800.0, totalWithInvalidCoupon, 0.001, "Invalid coupon must apply 0 discount");

        Booking bookingInvalidCoupon = bookingService.bookSeats(testCustomer.getUserId(), show2.getShowId(), seats2, "DISCOUNT50");
        assertEquals(800.0, bookingInvalidCoupon.getTotalAmount(), 0.001);
        assertEquals(0.0, bookingInvalidCoupon.getDiscountAmount(), 0.001);
        assertEquals(800.0, bookingInvalidCoupon.getFinalAmount(), 0.001);
    }

    /**
     * Requirement: Test booking summary displaying booking ID, movie, show time, selected seats, total amount.
     */
    @Test
    @Order(10)
    public void testBookingSummary() throws SQLException {
        Show show = createDedicatedTestShow();

        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("A7", "B7"));
        assertNotNull(booking);

        String summary = bookingService.getBookingSummary(booking.getBookingId());
        assertNotNull(summary);
        // Verify summary contains all mandatory information:
        // Booking ID, movie, show time, selected seats, total amount
        assertTrue(summary.contains("Booking ID") && summary.contains("#" + booking.getBookingId()), "Summary must contain booking ID");
        assertTrue(summary.contains(testMovie.getTitle()), "Summary must contain movie title");
        assertTrue(summary.contains("A7") && summary.contains("B7"), "Summary must display selected seats");
        assertTrue(summary.contains("300.00"), "Summary must display total amount");
        assertTrue(summary.contains("CONFIRMED"), "Summary must display status");

        // Verify Model's getSummary() matches
        assertEquals(summary, booking.getSummary());

        // Verify display method prints without exception
        assertDoesNotThrow(() -> bookingService.displayBookingSummary(booking.getBookingId()));
    }

    /**
     * Requirement: Test independent seats for different shows (Show 1 seat booking must not affect Show 2).
     */
    @Test
    @Order(11)
    public void testIndependentSeatsForDifferentShows() throws SQLException {
        Show show1 = createDedicatedTestShow();
        Show show2 = createDedicatedTestShow();

        assertNotEquals(show1.getShowId(), show2.getShowId());

        // Book seat D4 in Show 1
        bookingService.bookSeats(testCustomer.getUserId(), show1.getShowId(), List.of("D4"));

        // Show 1 seat D4 must be BOOKED
        assertFalse(seatService.isSeatAvailable(show1.getShowId(), "D4"), "Show 1 seat D4 must be BOOKED");

        // CRITICAL CHECK: Show 2 seat D4 must REMAIN AVAILABLE!
        assertTrue(seatService.isSeatAvailable(show2.getShowId(), "D4"), "Show 2 seat D4 must remain AVAILABLE independently");

        // We can now book seat D4 in Show 2 without conflict
        Booking bookingShow2 = bookingService.bookSeats(testCustomer.getUserId(), show2.getShowId(), List.of("D4"));
        assertNotNull(bookingShow2);
        assertFalse(seatService.isSeatAvailable(show2.getShowId(), "D4"), "Show 2 seat D4 is now also BOOKED independently");
    }

    /**
     * Requirement: Test cancellation coordination interfaces provided for Member 3.
     */
    @Test
    @Order(12)
    public void testCancellationIntegrationInterfacesForMember3() throws SQLException {
        Show show = createDedicatedTestShow();

        // 1. Verify Show implements Bookable interface
        assertTrue(show instanceof Bookable, "Show must implement Bookable interface");
        Bookable bookable = (Bookable) show;
        assertTrue(bookable.bookSeats());
        assertTrue(bookable.cancelBooking());

        // 2. Create a booking to test Member 3 interfaces
        Booking booking = bookingService.bookSeats(testCustomer.getUserId(), show.getShowId(), List.of("E1", "E2"));
        assertNotNull(booking);

        // Member 3 Interface 1: Lookup by booking ID
        Optional<Booking> fetchedById = bookingService.getBookingById(booking.getBookingId());
        assertTrue(fetchedById.isPresent());
        assertEquals(booking.getBookingCode(), fetchedById.get().getBookingCode());

        // Member 3 Interface 2: Lookup by booking code
        Optional<Booking> fetchedByCode = bookingService.getBookingByCode(booking.getBookingCode());
        assertTrue(fetchedByCode.isPresent());
        assertEquals(booking.getBookingId(), fetchedByCode.get().getBookingId());

        // Member 3 Interface 3: Retrieve line items for refund calculations
        List<BookingSeat> seats = bookingService.getBookingSeats(booking.getBookingId());
        assertEquals(2, seats.size());

        // Member 3 Interface 4: Update booking status to CANCELLED
        boolean statusUpdated = bookingDAO.updateBookingStatus(booking.getBookingId(), "CANCELLED");
        assertTrue(statusUpdated);

        Optional<Booking> cancelledBooking = bookingDAO.getBookingById(booking.getBookingId());
        assertTrue(cancelledBooking.isPresent());
        assertEquals("CANCELLED", cancelledBooking.get().getStatus());

        // Member 3 Interface 5: Release seats back to AVAILABLE
        for (BookingSeat bs : seats) {
            boolean seatFreed = seatDAO.updateShowSeatStatus(bs.getShowSeatId(), "AVAILABLE");
            assertTrue(seatFreed);
        }

        // Verify seats are now available again for re-booking
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "E1"));
        assertTrue(seatService.isSeatAvailable(show.getShowId(), "E2"));
    }
}
