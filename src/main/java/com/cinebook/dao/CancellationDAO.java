package com.cinebook.dao;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Cancellation;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for 'cancellations' table.
 *
 * Member 3 Responsibility:
 * - Invoking stored procedure 'sp_cancel_booking'.
 * - Querying cancellation and refund audit records.
 * - Retrieving enriched cancellation summaries.
 */
public class CancellationDAO {

    /**
     * Cancels a booking via the MySQL stored procedure 'sp_cancel_booking'.
     * The stored procedure:
     * 1. Verifies booking exists.
     * 2. Rejects already-cancelled bookings.
     * 3. Calculates 80% refund of final_amount.
     * 4. Releases associated show_seats back to AVAILABLE.
     * 5. Updates booking status to CANCELLED.
     * 6. Updates payment status to REFUNDED.
     * 7. Inserts a cancellation record with status COMPLETED.
     * 8. Returns the refund amount.
     *
     * @param bookingId primary booking ID
     * @return calculated refund amount
     * @throws SQLException on database error or validation error raised by the procedure
     */
    public double cancelBookingViaProcedure(int bookingId) throws SQLException {
        String sql = "{CALL sp_cancel_booking(?, ?)}";

        try (Connection conn = DBConnection.getConnection();
             CallableStatement cstmt = conn.prepareCall(sql)) {

            cstmt.setInt(1, bookingId);
            cstmt.registerOutParameter(2, Types.DECIMAL);

            boolean hasResultSet = cstmt.execute();

            // First check OUT parameter
            BigDecimal refundAmount = cstmt.getBigDecimal(2);

            // If OUT parameter is not set directly, attempt reading from returned ResultSet
            if (refundAmount == null && hasResultSet) {
                try (ResultSet rs = cstmt.getResultSet()) {
                    if (rs != null && rs.next()) {
                        refundAmount = rs.getBigDecimal("refund_amount");
                    }
                }
            }

            return refundAmount != null ? refundAmount.doubleValue() : 0.0;
        }
    }

    /**
     * Updates cancellation reason if a specific reason was supplied.
     *
     * @param bookingId booking identifier
     * @param reason    cancellation reason
     * @throws SQLException on database error
     */
    public void updateCancellationReason(int bookingId, String reason) throws SQLException {
        if (reason == null || reason.trim().isEmpty()) {
            return;
        }
        String sql = "UPDATE cancellations SET reason = ? WHERE booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reason.trim());
            pstmt.setInt(2, bookingId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves cancellation record by booking ID.
     *
     * @param bookingId booking identifier
     * @return Optional containing Cancellation entity
     * @throws SQLException on database error
     */
    public Optional<Cancellation> getCancellationByBookingId(int bookingId) throws SQLException {
        String sql = "SELECT c.cancellation_id, c.booking_id, c.refund_amount, c.cancelled_at, c.reason, c.status, "
                   + "       b.booking_code, b.final_amount AS original_final_amount, "
                   + "       m.title AS movie_title, u.full_name AS customer_name "
                   + "FROM cancellations c "
                   + "JOIN bookings b ON c.booking_id = b.booking_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "WHERE c.booking_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCancellation(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves cancellation record by primary cancellation ID.
     *
     * @param cancellationId cancellation identifier
     * @return Optional containing Cancellation entity
     * @throws SQLException on database error
     */
    public Optional<Cancellation> getCancellationById(int cancellationId) throws SQLException {
        String sql = "SELECT c.cancellation_id, c.booking_id, c.refund_amount, c.cancelled_at, c.reason, c.status, "
                   + "       b.booking_code, b.final_amount AS original_final_amount, "
                   + "       m.title AS movie_title, u.full_name AS customer_name "
                   + "FROM cancellations c "
                   + "JOIN bookings b ON c.booking_id = b.booking_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "WHERE c.cancellation_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cancellationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCancellation(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a booking already has a cancellation record.
     *
     * @param bookingId booking identifier
     * @return true if cancelled, false otherwise
     * @throws SQLException on database error
     */
    public boolean isBookingCancelled(int bookingId) throws SQLException {
        String sql = "SELECT 1 FROM cancellations WHERE booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Retrieves all cancellation records.
     *
     * @return list of cancellations
     * @throws SQLException on database error
     */
    public List<Cancellation> getAllCancellations() throws SQLException {
        List<Cancellation> list = new ArrayList<>();
        String sql = "SELECT c.cancellation_id, c.booking_id, c.refund_amount, c.cancelled_at, c.reason, c.status, "
                   + "       b.booking_code, b.final_amount AS original_final_amount, "
                   + "       m.title AS movie_title, u.full_name AS customer_name "
                   + "FROM cancellations c "
                   + "JOIN bookings b ON c.booking_id = b.booking_id "
                   + "JOIN shows s ON b.show_id = s.show_id "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN users u ON b.user_id = u.user_id "
                   + "ORDER BY c.cancelled_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToCancellation(rs));
            }
        }
        return list;
    }

    private Cancellation mapResultSetToCancellation(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("cancelled_at");
        Cancellation c = new Cancellation(
                rs.getInt("cancellation_id"),
                rs.getInt("booking_id"),
                rs.getDouble("refund_amount"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("reason"),
                rs.getString("status")
        );
        c.setBookingCode(rs.getString("booking_code"));
        c.setOriginalFinalAmount(rs.getDouble("original_final_amount"));
        c.setMovieTitle(rs.getString("movie_title"));
        c.setCustomerName(rs.getString("customer_name"));
        return c;
    }
}
