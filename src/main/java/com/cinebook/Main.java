package com.cinebook;

import com.cinebook.model.Booking;
import com.cinebook.model.Cancellation;
import com.cinebook.model.Movie;
import com.cinebook.model.Payment;
import com.cinebook.model.Show;
import com.cinebook.model.User;
import com.cinebook.service.BookingService;
import com.cinebook.service.CancellationService;
import com.cinebook.service.MovieService;
import com.cinebook.service.PaymentService;
import com.cinebook.service.SeatService;
import com.cinebook.service.ShowService;
import com.cinebook.service.UserService;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Main console application entry point for CineBook – Movie Ticket Booking System.
 *
 * Member 3 Responsibility:
 * - Central interactive terminal menu integrating Member 1, Member 2, and Member 3 modules.
 * - Handles registration, authentication, movie catalog, scheduling, seat maps,
 *   atomic seat bookings, simulated payments (CASH/UPI/CARD), booking histories,
 *   and procedure-backed cancellations with 80% refunds.
 */
public class Main {

    private final UserService userService;
    private final MovieService movieService;
    private final ShowService showService;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    private User currentUser;

    public Main() {
        this.userService = new UserService();
        this.movieService = new MovieService();
        this.showService = new ShowService();
        this.seatService = new SeatService();
        this.bookingService = new BookingService();
        this.paymentService = new PaymentService();
        this.cancellationService = new CancellationService();
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    /**
     * Main application loop displaying the console menu.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printHeader();

        while (running) {
            printMenu();
            int choice = readInt(scanner, "Enter your choice (1-10): ");

            try {
                switch (choice) {
                    case 1 -> handleRegister(scanner);
                    case 2 -> handleLogin(scanner);
                    case 3 -> handleViewMovies();
                    case 4 -> handleViewShows();
                    case 5 -> handleViewSeatMap(scanner);
                    case 6 -> handleBookTickets(scanner);
                    case 7 -> handleMakePayment(scanner);
                    case 8 -> handleViewBookingHistory();
                    case 9 -> handleCancelBooking(scanner);
                    case 10 -> {
                        System.out.println("\nThank you for choosing CineBook. Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("\n[!] Invalid option. Please enter a number between 1 and 10.");
                }
            } catch (Exception e) {
                System.out.println("\n[ERROR] " + e.getMessage());
            }

            if (running) {
                System.out.println("\nPress ENTER to continue...");
                scanner.nextLine();
            }
        }
    }

    private void printHeader() {
        System.out.println("=================================================================");
        System.out.println("            CINEBOOK - MOVIE TICKET BOOKING SYSTEM               ");
        System.out.println("             Java 17 | MySQL 8 | JDBC Architecture               ");
        System.out.println("=================================================================");
    }

    private void printMenu() {
        System.out.println("\n-----------------------------------------------------------------");
        if (currentUser != null) {
            System.out.printf(" Logged in as: %s (%s) | Role: %s%n",
                    currentUser.getFullName(), currentUser.getEmail(), currentUser.getRole());
        } else {
            System.out.println(" Logged in as: Guest (Please Register or Login to Book/Pay)");
        }
        System.out.println("-----------------------------------------------------------------");
        System.out.println(" 1.  Register");
        System.out.println(" 2.  Login");
        System.out.println(" 3.  View Movies");
        System.out.println(" 4.  View Shows");
        System.out.println(" 5.  View Seat Map");
        System.out.println(" 6.  Book Tickets");
        System.out.println(" 7.  Make Simulated Payment");
        System.out.println(" 8.  View Booking History");
        System.out.println(" 9.  Cancel Booking");
        System.out.println(" 10. Exit");
        System.out.println("-----------------------------------------------------------------");
    }

    // -----------------------------------------------------------------
    // 1. User Registration (Member 1 Integration)
    // -----------------------------------------------------------------
    private void handleRegister(Scanner scanner) throws SQLException {
        System.out.println("\n--- Customer Registration ---");
        String name = readString(scanner, "Enter Full Name: ");
        String email = readString(scanner, "Enter Email: ");
        String password = readString(scanner, "Enter Password (min 6 chars): ");

        User newUser = userService.registerCustomer(name, email, password);
        System.out.println("\n[SUCCESS] Registration successful! Welcome, " + newUser.getFullName() + ".");
        this.currentUser = newUser;
    }

    // -----------------------------------------------------------------
    // 2. User Authentication (Member 1 Integration)
    // -----------------------------------------------------------------
    private void handleLogin(Scanner scanner) throws SQLException {
        System.out.println("\n--- User Login ---");
        String email = readString(scanner, "Enter Email: ");
        String password = readString(scanner, "Enter Password: ");

        User user = userService.login(email, password);
        this.currentUser = user;
        System.out.println("\n[SUCCESS] Login successful! Welcome back, " + user.getFullName() + ".");
    }

    // -----------------------------------------------------------------
    // 3. View Movies (Member 1 Integration)
    // -----------------------------------------------------------------
    private void handleViewMovies() throws SQLException {
        System.out.println("\n======================= MOVIE CATALOG =======================");
        List<Movie> movies = movieService.getAllMovies();
        if (movies.isEmpty()) {
            System.out.println("No movies currently screening.");
            return;
        }

        System.out.printf(Locale.ROOT, "%-5s | %-25s | %-10s | %-12s | %-8s | %-12s%n",
                "ID", "Title", "Language", "Genre", "Duration", "Release Date");
        System.out.println("-----------------------------------------------------------------------------");
        for (Movie m : movies) {
            System.out.printf(Locale.ROOT, "%-5d | %-25s | %-10s | %-12s | %3d mins | %-12s%n",
                    m.getMovieId(),
                    truncate(m.getTitle(), 25),
                    truncate(m.getLanguage(), 10),
                    truncate(m.getGenre(), 12),
                    m.getDurationMinutes(),
                    m.getReleaseDate());
        }
        System.out.println("=============================================================================");
    }

    // -----------------------------------------------------------------
    // 4. View Scheduled Shows (Member 1 Integration)
    // -----------------------------------------------------------------
    private void handleViewShows() throws SQLException {
        System.out.println("\n=========================== SCHEDULED SHOWS ===========================");
        List<Show> shows = showService.getAllShows();
        if (shows.isEmpty()) {
            System.out.println("No shows currently scheduled.");
            return;
        }

        System.out.printf(Locale.ROOT, "%-6s | %-22s | %-18s | %-10s | %-12s | %-8s%n",
                "ShowID", "Movie", "Theatre", "Screen", "Date", "Time");
        System.out.println("---------------------------------------------------------------------------------------");
        for (Show s : shows) {
            System.out.printf(Locale.ROOT, "%-6d | %-22s | %-18s | %-10s | %-12s | %-8s%n",
                    s.getShowId(),
                    truncate(s.getMovieTitle(), 22),
                    truncate(s.getTheatreName(), 18),
                    truncate(s.getScreenName(), 10),
                    s.getShowDate(),
                    s.getShowTime());
        }
        System.out.println("=======================================================================================");
    }

    // -----------------------------------------------------------------
    // 5. View Seat Map (Member 2 Integration)
    // -----------------------------------------------------------------
    private void handleViewSeatMap(Scanner scanner) throws SQLException {
        int showId = readInt(scanner, "Enter Show ID: ");
        Optional<Show> showOpt = showService.getShowById(showId);
        if (showOpt.isEmpty()) {
            System.out.println("[!] Show #" + showId + " does not exist.");
            return;
        }

        Show show = showOpt.get();
        System.out.printf("%nScreening: %s (%s) at %s - %s [%s %s]%n",
                show.getMovieTitle(), show.getMovieLanguage(),
                show.getTheatreName(), show.getScreenName(),
                show.getShowDate(), show.getShowTime());

        seatService.displaySeatMap(showId);
    }

    // -----------------------------------------------------------------
    // 6. Book Tickets (Member 2 Integration with Trigger Coordination)
    // -----------------------------------------------------------------
    private void handleBookTickets(Scanner scanner) throws SQLException {
        if (!requireLogin()) {
            return;
        }

        int showId = readInt(scanner, "Enter Show ID to book: ");
        Optional<Show> showOpt = showService.getShowById(showId);
        if (showOpt.isEmpty()) {
            System.out.println("[!] Show #" + showId + " does not exist.");
            return;
        }

        Show show = showOpt.get();
        System.out.printf("%nScreening: %s at %s - %s [%s %s]%n",
                show.getMovieTitle(), show.getTheatreName(),
                show.getScreenName(), show.getShowDate(), show.getShowTime());

        // Display current seat availability map
        seatService.displaySeatMap(showId);

        String seatInput = readString(scanner, "Enter seats separated by commas (e.g. A1, A2): ");
        List<String> selectedSeats = Arrays.stream(seatInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (selectedSeats.isEmpty()) {
            System.out.println("[!] No seats entered.");
            return;
        }

        String coupon = readString(scanner, "Enter promotional coupon code (or press Enter to skip): ");
        if (coupon.trim().isEmpty()) {
            coupon = null;
        }

        // Preview calculated total
        double calculatedTotal = bookingService.calculateTotal(selectedSeats, coupon);
        System.out.printf(Locale.ROOT, "%nCalculated Ticket Amount: Rs. %.2f%n", calculatedTotal);

        String confirmBooking = readString(scanner, "Confirm booking creation? (Y/N): ");
        if (!confirmBooking.equalsIgnoreCase("Y")) {
            System.out.println("Booking cancelled by user.");
            return;
        }

        // Atomic booking execution via Member 2 BookingService
        Booking booking = bookingService.bookSeats(currentUser.getUserId(), showId, selectedSeats, coupon);
        System.out.println("\n[SUCCESS] Seats booked successfully!");
        System.out.println(booking.getSummary());

        // Seamless handover to Simulated Payment Workflow
        String proceedPay = readString(scanner, "Proceed to simulated payment now? (Y/N): ");
        if (proceedPay.equalsIgnoreCase("Y")) {
            executePaymentWorkflow(scanner, booking.getBookingId(), booking.getFinalAmount());
        } else {
            System.out.println("You can complete payment later via Option 7 (Make Simulated Payment).");
        }
    }

    // -----------------------------------------------------------------
    // 7. Make Simulated Payment (Member 3 Payment Module)
    // -----------------------------------------------------------------
    private void handleMakePayment(Scanner scanner) throws SQLException {
        if (!requireLogin()) {
            return;
        }

        int bookingId = readInt(scanner, "Enter Booking ID to pay for: ");
        Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
        if (bookingOpt.isEmpty()) {
            System.out.println("[!] Booking #" + bookingId + " does not exist.");
            return;
        }

        Booking booking = bookingOpt.get();
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            System.out.println("[!] Booking #" + bookingId + " is cancelled. Payment cannot be made.");
            return;
        }

        Optional<Payment> existingPayment = paymentService.getPaymentByBookingId(bookingId);
        if (existingPayment.isPresent() && existingPayment.get().isSuccess()) {
            System.out.println("[!] Booking #" + bookingId + " has already been paid successfully.");
            paymentService.displayReceipt(existingPayment.get());
            return;
        }

        executePaymentWorkflow(scanner, bookingId, booking.getFinalAmount());
    }

    private void executePaymentWorkflow(Scanner scanner, int bookingId, double amount) throws SQLException {
        System.out.println("\n=======================================================");
        System.out.println("               SIMULATED PAYMENT GATEWAY               ");
        System.out.println("=======================================================");
        System.out.printf(Locale.ROOT, " Booking ID      : #%d%n", bookingId);
        System.out.printf(Locale.ROOT, " Payable Amount  : Rs. %.2f%n", amount);
        System.out.println("-------------------------------------------------------");
        System.out.println(" Select Payment Method:");
        System.out.println(" 1. CASH");
        System.out.println(" 2. UPI");
        System.out.println(" 3. CARD");
        System.out.println("-------------------------------------------------------");
        System.out.println(" [SIMULATION MODE] No real card numbers or UPI PINs needed.");

        String methodInput = readString(scanner, "Choose payment method (1-3 or CASH/UPI/CARD): ");
        String selectedMethod = paymentService.normalizePaymentMethod(methodInput);

        System.out.printf(Locale.ROOT, "%nSimulated Payment of Rs. %.2f via [%s]%n", amount, selectedMethod);
        String confirmation = readString(scanner, "Confirm simulated payment? (Y/N): ");
        boolean confirmed = confirmation.equalsIgnoreCase("Y");

        Payment payment = paymentService.processPayment(bookingId, selectedMethod, confirmed);

        if (payment.isSuccess()) {
            System.out.println("\n[SUCCESS] Payment simulation successful!");
            paymentService.displayReceipt(payment);
        } else {
            System.out.println("\n[NOTICE] Payment was rejected or not confirmed.");
            System.out.println("Transaction Status: FAILED. Booking remains unpaid.");
        }
    }

    // -----------------------------------------------------------------
    // 8. View Booking History (Member 2 + Member 3 Integration)
    // -----------------------------------------------------------------
    private void handleViewBookingHistory() throws SQLException {
        if (!requireLogin()) {
            return;
        }

        System.out.println("\n============================ YOUR BOOKING HISTORY ============================");
        List<Booking> bookings = bookingService.getBookingsByUserId(currentUser.getUserId());
        if (bookings.isEmpty()) {
            System.out.println("No bookings found for your account.");
            return;
        }

        System.out.printf(Locale.ROOT, "%-6s | %-10s | %-20s | %-12s | %-10s | %-9s | %-10s | %-9s%n",
                "BookID", "Code", "Movie", "Date", "Seats", "Final Amt", "Status", "Payment");
        System.out.println("--------------------------------------------------------------------------------------------------");
        for (Booking b : bookings) {
            Optional<Payment> pmtOpt = paymentService.getPaymentByBookingId(b.getBookingId());
            String pmtStatus = pmtOpt.map(Payment::getPaymentStatus).orElse("UNPAID");

            String seatsDisplay = (b.getSeatCodes() != null && !b.getSeatCodes().isEmpty())
                    ? String.join(",", b.getSeatCodes()) : "N/A";

            System.out.printf(Locale.ROOT, "%-6d | %-10s | %-20s | %-12s | %-10s | Rs.%-6.2f | %-10s | %-9s%n",
                    b.getBookingId(),
                    b.getBookingCode(),
                    truncate(b.getMovieTitle(), 20),
                    b.getShowDate() != null ? b.getShowDate().toString() : "N/A",
                    truncate(seatsDisplay, 10),
                    b.getFinalAmount(),
                    b.getStatus(),
                    pmtStatus);
        }
        System.out.println("==================================================================================================");
    }

    // -----------------------------------------------------------------
    // 9. Cancel Booking (Member 3 Stored Procedure Integration)
    // -----------------------------------------------------------------
    private void handleCancelBooking(Scanner scanner) throws SQLException {
        if (!requireLogin()) {
            return;
        }

        int bookingId = readInt(scanner, "Enter Booking ID to cancel: ");
        Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
        if (bookingOpt.isEmpty()) {
            System.out.println("[!] Booking #" + bookingId + " does not exist.");
            return;
        }

        Booking booking = bookingOpt.get();
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            System.out.println("[!] Booking #" + bookingId + " is already cancelled. Duplicate cancellation rejected.");
            return;
        }

        double expectedRefund = Math.round(booking.getFinalAmount() * 0.80 * 100.0) / 100.0;
        System.out.printf(Locale.ROOT, "%nBooking Code   : %s%n", booking.getBookingCode());
        System.out.printf(Locale.ROOT, "Movie          : %s%n", booking.getMovieTitle());
        System.out.printf(Locale.ROOT, "Final Amount   : Rs. %.2f%n", booking.getFinalAmount());
        System.out.printf(Locale.ROOT, "Refund (80%%)   : Rs. %.2f%n", expectedRefund);

        String reason = readString(scanner, "Enter reason for cancellation (or press Enter for default): ");
        if (reason.trim().isEmpty()) {
            reason = "Customer requested cancellation (80% refund policy)";
        }

        String confirmCancel = readString(scanner, "Are you sure you want to cancel this booking? (Y/N): ");
        if (!confirmCancel.equalsIgnoreCase("Y")) {
            System.out.println("Cancellation aborted.");
            return;
        }

        // Execute stored procedure 'sp_cancel_booking' via CancellationService
        Cancellation cancellation = cancellationService.cancelBooking(bookingId, reason);
        System.out.println("\n[SUCCESS] Booking cancelled successfully! Seats released back to available pool.");
        cancellationService.displayReceipt(cancellation);
    }

    // -----------------------------------------------------------------
    // Helper Utilities
    // -----------------------------------------------------------------
    private boolean requireLogin() {
        if (currentUser == null) {
            System.out.println("[!] Authentication required. Please Register (Option 1) or Login (Option 2) first.");
            return false;
        }
        return true;
    }

    private int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[!] Please enter a valid integer number.");
            }
        }
    }

    private String readString(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "N/A";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 2) + "..";
    }
}
