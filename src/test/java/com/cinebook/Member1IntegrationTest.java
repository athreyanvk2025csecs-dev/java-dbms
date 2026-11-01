package com.cinebook;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Movie;
import com.cinebook.model.Screen;
import com.cinebook.model.Show;
import com.cinebook.model.ShowSeat;
import com.cinebook.model.Theatre;
import com.cinebook.model.User;
import com.cinebook.service.MovieService;
import com.cinebook.service.ShowService;
import com.cinebook.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Member1IntegrationTest {

    private static UserService userService;
    private static MovieService movieService;
    private static ShowService showService;

    @BeforeAll
    public static void setup() {
        assertTrue(DBConnection.testConnection(), "Database connection must be valid and active");
        userService = new UserService();
        movieService = new MovieService();
        showService = new ShowService();
    }

    @Test
    @Order(1)
    public void testDatabaseConnection() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn, "Connection object must not be null");
            assertFalse(conn.isClosed(), "Connection must be open");
            assertTrue(conn.isValid(2), "Connection must be responsive within 2 seconds");
        }
    }

    @Test
    @Order(2)
    public void testUserRegistrationAndLogin() throws SQLException {
        String testEmail = "test_user_" + System.currentTimeMillis() + "@cinebook.com";
        String password = "SecurePassword123";

        // Register customer
        User user = userService.registerCustomer("Test Customer", testEmail, password);
        assertNotNull(user, "User should be registered");
        assertTrue(user.getUserId() > 0, "Generated user ID should be > 0");
        assertEquals("CUSTOMER", user.getRole());
        assertEquals(testEmail.toLowerCase(), user.getEmail());

        // Duplicate email prevention test
        IllegalArgumentException dupEx = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerCustomer("Duplicate Person", testEmail, "AnotherPass123"),
                "Registering with an existing email should throw IllegalArgumentException"
        );
        assertTrue(dupEx.getMessage().contains("already registered"));

        // Login with correct credentials
        User loggedInUser = userService.login(testEmail, password);
        assertNotNull(loggedInUser);
        assertEquals(user.getUserId(), loggedInUser.getUserId());

        // Login with wrong password
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(testEmail, "WrongPassword"),
                "Login with wrong password must throw IllegalArgumentException"
        );
    }

    @Test
    @Order(3)
    public void testMovieManagement() throws SQLException {
        Movie movie = movieService.addMovie(
                "Oppenheimer",
                "English",
                "Biography / Drama / History",
                180,
                LocalDate.of(2023, 7, 21)
        );

        assertNotNull(movie);
        assertTrue(movie.getMovieId() > 0);

        // Retrieve by ID
        Optional<Movie> fetched = movieService.getMovieById(movie.getMovieId());
        assertTrue(fetched.isPresent());
        assertEquals("Oppenheimer", fetched.get().getTitle());

        // Search by title
        List<Movie> searchResults = movieService.searchMovies("oppen");
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.stream().anyMatch(m -> m.getTitle().equals("Oppenheimer")));
    }

    @Test
    @Order(4)
    public void testTheatreAndScreenManagementWithSeats() throws SQLException {
        // Add Theatre
        Theatre theatre = movieService.addTheatre(
                "PVR VR Mall",
                "Chennai",
                "Jawaharlal Nehru Road, Anna Nagar, Chennai 600040"
        );
        assertNotNull(theatre);
        assertTrue(theatre.getTheatreId() > 0);

        // Add Screen (auto-provisions 40 seats: 5x8 grid)
        Screen screen = movieService.addScreen(theatre.getTheatreId(), "Audi 1 (Dolby Cinema)");
        assertNotNull(screen);
        assertTrue(screen.getScreenId() > 0);
        assertEquals(40, screen.getSeats().size(), "Screen must have 40 seats automatically provisioned");

        // Verify seats associated with screen
        List<com.cinebook.model.Seat> seats = movieService.getSeatsForScreen(screen.getScreenId());
        assertEquals(40, seats.size());
        assertTrue(seats.stream().anyMatch(s -> s.getSeatCode().equals("A1") && s.getSeatType().equals("REGULAR")));
        assertTrue(seats.stream().anyMatch(s -> s.getSeatCode().equals("C4") && s.getSeatType().equals("PREMIUM")));
        assertTrue(seats.stream().anyMatch(s -> s.getSeatCode().equals("E8") && s.getSeatType().equals("VIP")));
    }

    @Test
    @Order(5)
    public void testShowSchedulingAndIndependentSeatAvailability() throws SQLException {
        // Create sample movie, theatre, and screen
        Movie movie = movieService.addMovie(
                "Gladiator II",
                "English",
                "Action / Adventure",
                148,
                LocalDate.of(2024, 11, 22)
        );

        Theatre theatre = movieService.addTheatre(
                "SPI Luxe",
                "Chennai",
                "Phoenix Market City, Velachery, Chennai"
        );

        Screen screen = movieService.addScreen(theatre.getTheatreId(), "Screen 1");

        // Schedule Show 1
        LocalDate showDate = LocalDate.now().plusDays(1);
        LocalTime showTime1 = LocalTime.of(15, 0);
        Show show1 = showService.scheduleShow(movie.getMovieId(), screen.getScreenId(), showDate, showTime1);
        assertNotNull(show1);
        assertTrue(show1.getShowId() > 0);

        // Schedule Show 2 at same screen but different time
        LocalTime showTime2 = LocalTime.of(19, 0);
        Show show2 = showService.scheduleShow(movie.getMovieId(), screen.getScreenId(), showDate, showTime2);
        assertNotNull(show2);
        assertTrue(show2.getShowId() > 0);

        // Verify Show 1 has 40 independent show_seats generated
        List<ShowSeat> show1Seats = showService.getShowSeats(show1.getShowId());
        assertEquals(40, show1Seats.size(), "Show 1 must have 40 independent show_seats records");
        assertTrue(show1Seats.stream().allMatch(ShowSeat::isAvailable), "All seats initially AVAILABLE");

        // Book seat C3 in Show 1
        ShowSeat seatC3Show1 = show1Seats.stream()
                .filter(s -> "C3".equals(s.getSeatCode()))
                .findFirst()
                .orElseThrow();
        showService.updateSeatStatus(seatC3Show1.getShowSeatId(), "BOOKED");

        // Verify Show 1 seat C3 is BOOKED
        List<ShowSeat> reloadedShow1Seats = showService.getShowSeats(show1.getShowId());
        ShowSeat reloadedC3Show1 = reloadedShow1Seats.stream()
                .filter(s -> "C3".equals(s.getSeatCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("BOOKED", reloadedC3Show1.getStatus());

        // CRITICAL CHECK: Show 2 seat C3 must REMAIN AVAILABLE (independent seat availability record!)
        List<ShowSeat> show2Seats = showService.getShowSeats(show2.getShowId());
        assertEquals(40, show2Seats.size());
        ShowSeat seatC3Show2 = show2Seats.stream()
                .filter(s -> "C3".equals(s.getSeatCode()))
                .findFirst()
                .orElseThrow();
        assertEquals("AVAILABLE", seatC3Show2.getStatus(), "Show 2 seat C3 must remain AVAILABLE independently");

        // Verify seat map rendering
        String seatMap = showService.renderSeatMap(show1.getShowId());
        assertNotNull(seatMap);
        assertTrue(seatMap.contains("[X]"), "Seat map should show [X] for the booked seat");
        assertTrue(seatMap.contains("[O]"), "Seat map should show [O] for available seats");
    }
}
