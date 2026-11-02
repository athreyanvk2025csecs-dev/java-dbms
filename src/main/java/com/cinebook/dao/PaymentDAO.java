package com.cinebook.dao;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for 'payments' table.
 *
 * Member 3 Responsibility:
 * - Persisting simulated payment transactions.
 * - Retrieving payment records by payment ID, booking ID, and user ID.
 * - Updating payment status (e.g. to 'REFUNDED' or 'FAILED').
 */
public class PaymentDAO {

    /**
     * Inserts a payment record into 'payments' table using an active connection.
     *
     * @param conn    active connection
     * @param payment Payment model
     * @return generated payment_id
     * @throws SQLException on database error
     */
    public int insertPayment(Connection conn, Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (booking_id, payment_method, payment_status, amount, paid_at) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, payment.getBookingId());
            pstmt.setString(2, payment.getPaymentMethod() != null ? payment.getPaymentMethod() : Payment.METHOD_CASH);
            pstmt.setString(3, payment.getPaymentStatus() != null ? payment.getPaymentStatus() : Payment.STATUS_SUCCESS);
            pstmt.setDouble(4, payment.getAmount());

            LocalDateTime paidAt = payment.getPaidAt() != null ? payment.getPaidAt() : LocalDateTime.now();
            pstmt.setTimestamp(5, Timestamp.valueOf(paidAt));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating payment record failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    payment.setPaymentId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating payment record failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Standalone insertion of a payment record.
     *
     * @param payment Payment model
     * @return generated payment_id
     * @throws SQLException on database error
     */
    public int insertPayment(Payment payment) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return insertPayment(conn, payment);
        }
    }

    /**
     * Retrieves a payment by its primary ID.
     *
     * @param paymentId payment identifier
     * @return Optional containing enriched Payment
     * @throws SQLException on database error
     */
    public Optional<Payment> getPaymentById(int paymentId) throws SQLException {
        String sql = "SELECT p.payment_id, p.booking_id, p.payment_method, p.payment_status, p.amount, p.paid_at, "
                   + "       b.booking_code, m.title AS movie_title, u.full_name AS customer_name "
                   + "FROM payments p "
                   + "JOIN bookings b ON p.booking_id = b.booking_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "WHERE p.payment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, paymentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPayment(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves a payment associated with a booking ID.
     *
     * @param bookingId booking identifier
     * @return Optional containing enriched Payment
     * @throws SQLException on database error
     */
    public Optional<Payment> getPaymentByBookingId(int bookingId) throws SQLException {
        String sql = "SELECT p.payment_id, p.booking_id, p.payment_method, p.payment_status, p.amount, p.paid_at, "
                   + "       b.booking_code, m.title AS movie_title, u.full_name AS customer_name "
                   + "FROM payments p "
                   + "JOIN bookings b ON p.booking_id = b.booking_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "WHERE p.booking_id = ? "
                   + "ORDER BY p.payment_id DESC "
                   + "LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPayment(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all payments for a specific user.
     *
     * @param userId user identifier
     * @return list of payments
     * @throws SQLException on database error
     */
    public List<Payment> getPaymentsByUserId(int userId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.booking_id, p.payment_method, p.payment_status, p.amount, p.paid_at, "
                   + "       b.booking_code, m.title AS movie_title, u.full_name AS customer_name "
                   + "FROM payments p "
                   + "JOIN bookings b ON p.booking_id = b.booking_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "WHERE b.user_id = ? "
                   + "ORDER BY p.paid_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayment(rs));
                }
            }
        }
        return list;
    }

    /**
     * Updates payment status for a booking within an active connection.
     *
     * @param conn      active connection
     * @param bookingId booking identifier
     * @param status    new payment status ('SUCCESS', 'REFUNDED', 'FAILED')
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean updatePaymentStatus(Connection conn, int bookingId, String status) throws SQLException {
        String sql = "UPDATE payments SET payment_status = ? WHERE booking_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.trim().toUpperCase());
            pstmt.setInt(2, bookingId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Standalone update of payment status.
     *
     * @param bookingId booking identifier
     * @param status    new payment status ('SUCCESS', 'REFUNDED', 'FAILED')
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean updatePaymentStatus(int bookingId, String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return updatePaymentStatus(conn, bookingId, status);
        }
    }

    /**
     * Retrieves all payment transactions.
     *
     * @return list of all payments
     * @throws SQLException on database error
     */
    public List<Payment> getAllPayments() throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.booking_id, p.payment_method, p.payment_status, p.amount, p.paid_at, "
                   + "       b.booking_code, m.title AS movie_title, u.full_name AS customer_name "
                   + "FROM payments p "
                   + "JOIN bookings b ON p.booking_id = b.booking_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "ORDER BY p.paid_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPayment(rs));
            }
        }
        return list;
    }

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("paid_at");
        Payment p = new Payment(
                rs.getInt("payment_id"),
                rs.getInt("booking_id"),
                rs.getString("payment_method"),
                rs.getString("payment_status"),
                rs.getDouble("amount"),
                ts != null ? ts.toLocalDateTime() : null
        );
        p.setBookingCode(rs.getString("booking_code"));
        p.setMovieTitle(rs.getString("movie_title"));
        p.setCustomerName(rs.getString("customer_name"));
        return p;
    }
}
