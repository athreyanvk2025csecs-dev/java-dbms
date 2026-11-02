package com.cinebook.dao;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Booking;
import com.cinebook.model.BookingSeat;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for 'bookings' and 'booking_seats' tables.
 *
 * Member 2 Responsibility:
 * - Persisting bookings and line-item booking seats in database.
 * - Generating unique BK1001-style booking codes.
 * - Enforcing database-generated booking_id as permanent identity.
 * - Providing database query and update interfaces for Member 3's cancellation module.
 */
public class BookingDAO {

    /**
     * Generates a unique booking code in the agreed BK1001-style format.
     * Looks up existing maximum booking ID or highest BK code number to generate next sequential code.
     *
     * @param conn active database connection
     * @return unique booking code (e.g. BK1001, BK1002, ...)
     * @throws SQLException on database query error
     */
    public String generateBookingCode(Connection conn) throws SQLException {
        String sql = "SELECT MAX(booking_id) FROM bookings";
        int maxId = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                maxId = rs.getInt(1);
            }
        }

        int candidateNum = 1000 + maxId + 1;
        String candidateCode = "BK" + candidateNum;

        // Ensure absolute uniqueness in the bookings table
        while (isBookingCodeTaken(conn, candidateCode)) {
            candidateNum++;
            candidateCode = "BK" + candidateNum;
        }

        return candidateCode;
    }

    /**
     * Checks if a booking code already exists in the bookings table.
     *
     * @param conn active connection
     * @param code candidate code
     * @return true if code is already taken, false otherwise
     * @throws SQLException on database error
     */
    public boolean isBookingCodeTaken(Connection conn, String code) throws SQLException {
        String sql = "SELECT 1 FROM bookings WHERE booking_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Inserts a new booking record within an active transaction.
     * Sets the auto-generated booking_id and assigned booking_code on the Booking object.
     *
     * @param conn    active JDBC transaction connection
     * @param booking Booking model containing details
     * @return generated booking_id
     * @throws SQLException on insert error
     */
    public int insertBooking(Connection conn, Booking booking) throws SQLException {
        if (booking.getBookingCode() == null || booking.getBookingCode().trim().isEmpty()) {
            booking.setBookingCode(generateBookingCode(conn));
        }

        String sql = "INSERT INTO bookings (booking_code, user_id, show_id, booking_datetime, "
                   + "                      total_amount, discount_amount, final_amount, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, booking.getBookingCode());
            pstmt.setInt(2, booking.getUserId());
            pstmt.setInt(3, booking.getShowId());

            LocalDateTime dt = booking.getBookingDateTime() != null ? booking.getBookingDateTime() : LocalDateTime.now();
            pstmt.setTimestamp(4, Timestamp.valueOf(dt));

            pstmt.setDouble(5, booking.getTotalAmount());
            pstmt.setDouble(6, booking.getDiscountAmount());
            pstmt.setDouble(7, booking.getFinalAmount());
            pstmt.setString(8, booking.getStatus() != null ? booking.getStatus() : "CONFIRMED");

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating booking failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    booking.setBookingId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating booking failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Batch inserts individual booking seats records in 'booking_seats' table.
     *
     * @param conn         active JDBC transaction connection
     * @param bookingSeats list of BookingSeat entities
     * @throws SQLException on database error
     */
    public void insertBookingSeatsBatch(Connection conn, List<BookingSeat> bookingSeats) throws SQLException {
        if (bookingSeats == null || bookingSeats.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO booking_seats (booking_id, show_seat_id, seat_price) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (BookingSeat bs : bookingSeats) {
                pstmt.setInt(1, bs.getBookingId());
                pstmt.setInt(2, bs.getShowSeatId());
                pstmt.setDouble(3, bs.getSeatPrice());
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * Retrieves a booking by its primary database ID with joined show, movie, and seat details.
     *
     * @param bookingId booking primary key
     * @return Optional containing fully enriched Booking
     * @throws SQLException on database error
     */
    public Optional<Booking> getBookingById(int bookingId) throws SQLException {
        String sql = "SELECT b.booking_id, b.booking_code, b.user_id, b.show_id, b.booking_datetime, "
                   + "       b.total_amount, b.discount_amount, b.final_amount, b.status, "
                   + "       u.full_name AS user_name, u.email AS user_email, "
                   + "       m.title AS movie_title, m.language AS movie_language, "
                   + "       t.name AS theatre_name, t.city AS theatre_city, "
                   + "       sc.screen_name, s.show_date, s.show_time "
                   + "FROM bookings b "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN screens sc ON s.screen_id = sc.screen_id "
                   + "JOIN theatres t ON sc.theatre_id = t.theatre_id "
                   + "WHERE b.booking_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Booking booking = mapResultSetToBooking(rs);
                    loadBookingSeatsAndCodes(conn, booking);
                    return Optional.of(booking);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves a booking by its unique booking code (e.g. BK1001).
     *
     * @param bookingCode unique booking code
     * @return Optional containing fully enriched Booking
     * @throws SQLException on database error
     */
    public Optional<Booking> getBookingByCode(String bookingCode) throws SQLException {
        String sql = "SELECT b.booking_id, b.booking_code, b.user_id, b.show_id, b.booking_datetime, "
                   + "       b.total_amount, b.discount_amount, b.final_amount, b.status, "
                   + "       u.full_name AS user_name, u.email AS user_email, "
                   + "       m.title AS movie_title, m.language AS movie_language, "
                   + "       t.name AS theatre_name, t.city AS theatre_city, "
                   + "       sc.screen_name, s.show_date, s.show_time "
                   + "FROM bookings b "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN screens sc ON s.screen_id = sc.screen_id "
                   + "JOIN theatres t ON sc.theatre_id = t.theatre_id "
                   + "WHERE b.booking_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bookingCode.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Booking booking = mapResultSetToBooking(rs);
                    loadBookingSeatsAndCodes(conn, booking);
                    return Optional.of(booking);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all bookings made by a specific user.
     *
     * @param userId user identifier
     * @return list of bookings for the user
     * @throws SQLException on database error
     */
    public List<Booking> getBookingsByUserId(int userId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.booking_code, b.user_id, b.show_id, b.booking_datetime, "
                   + "       b.total_amount, b.discount_amount, b.final_amount, b.status, "
                   + "       u.full_name AS user_name, u.email AS user_email, "
                   + "       m.title AS movie_title, m.language AS movie_language, "
                   + "       t.name AS theatre_name, t.city AS theatre_city, "
                   + "       sc.screen_name, s.show_date, s.show_time "
                   + "FROM bookings b "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN screens sc ON s.screen_id = sc.screen_id "
                   + "JOIN theatres t ON sc.theatre_id = t.theatre_id "
                   + "WHERE b.user_id = ? "
                   + "ORDER BY b.booking_datetime DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Booking booking = mapResultSetToBooking(rs);
                    loadBookingSeatsAndCodes(conn, booking);
                    list.add(booking);
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all booking seat items associated with a booking ID.
     * Used by Member 3 for cancellation and refunds.
     *
     * @param bookingId booking identifier
     * @return list of BookingSeat records
     * @throws SQLException on database error
     */
    public List<BookingSeat> getBookingSeatsByBookingId(int bookingId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return getBookingSeatsByBookingId(conn, bookingId);
        }
    }

    /**
     * Retrieves all booking seat items associated with a booking ID within an active transaction.
     *
     * @param conn      active JDBC connection
     * @param bookingId booking identifier
     * @return list of BookingSeat records
     * @throws SQLException on database error
     */
    public List<BookingSeat> getBookingSeatsByBookingId(Connection conn, int bookingId) throws SQLException {
        List<BookingSeat> list = new ArrayList<>();
        String sql = "SELECT bs.booking_seat_id, bs.booking_id, bs.show_seat_id, bs.seat_price, "
                   + "       s.seat_code, s.seat_type "
                   + "FROM booking_seats bs "
                   + "JOIN show_seats ss ON bs.show_seat_id = ss.show_seat_id "
                   + "JOIN seats s ON ss.seat_id = s.seat_id "
                   + "WHERE bs.booking_id = ? "
                   + "ORDER BY s.seat_code ASC";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BookingSeat bs = new BookingSeat(
                            rs.getInt("booking_seat_id"),
                            rs.getInt("booking_id"),
                            rs.getInt("show_seat_id"),
                            rs.getDouble("seat_price")
                    );
                    bs.setSeatCode(rs.getString("seat_code"));
                    bs.setSeatType(rs.getString("seat_type"));
                    list.add(bs);
                }
            }
        }
        return list;
    }

    /**
     * Updates booking status within an active transaction.
     * Used by Member 3 when cancelling a booking (e.g. status = 'CANCELLED').
     *
     * @param conn      active JDBC connection
     * @param bookingId booking identifier
     * @param status    new status ('CONFIRMED', 'CANCELLED', 'PENDING')
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean updateBookingStatus(Connection conn, int bookingId, String status) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.trim().toUpperCase());
            pstmt.setInt(2, bookingId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Standalone update of booking status (auto-commit).
     *
     * @param bookingId booking identifier
     * @param status    new status ('CONFIRMED', 'CANCELLED', 'PENDING')
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean updateBookingStatus(int bookingId, String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return updateBookingStatus(conn, bookingId, status);
        }
    }

    private void loadBookingSeatsAndCodes(Connection conn, Booking booking) throws SQLException {
        List<BookingSeat> seats = getBookingSeatsByBookingId(conn, booking.getBookingId());
        booking.setBookingSeats(seats);
        List<String> codes = new ArrayList<>();
        for (BookingSeat bs : seats) {
            if (bs.getSeatCode() != null) {
                codes.add(bs.getSeatCode());
            }
        }
        booking.setSeatCodes(codes);
    }

    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("booking_datetime");
        Booking b = new Booking(
                rs.getInt("booking_id"),
                rs.getString("booking_code"),
                rs.getInt("user_id"),
                rs.getInt("show_id"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getDouble("total_amount"),
                rs.getDouble("discount_amount"),
                rs.getDouble("final_amount"),
                rs.getString("status")
        );
        b.setUserName(rs.getString("user_name"));
        b.setUserEmail(rs.getString("user_email"));
        b.setMovieTitle(rs.getString("movie_title"));
        b.setMovieLanguage(rs.getString("movie_language"));
        b.setTheatreName(rs.getString("theatre_name"));
        b.setTheatreCity(rs.getString("theatre_city"));
        b.setScreenName(rs.getString("screen_name"));

        Date showDate = rs.getDate("show_date");
        Time showTime = rs.getTime("show_time");
        if (showDate != null) b.setShowDate(showDate.toLocalDate());
        if (showTime != null) b.setShowTime(showTime.toLocalTime());

        return b;
    }
}
