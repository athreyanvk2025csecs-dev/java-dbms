package com.cinebook.dao;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Seat;
import com.cinebook.model.ShowSeat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for seat management and live show seat tracking.
 * Interacts with 'seats' and 'show_seats' tables.
 *
 * Member 2 Responsibility:
 * - Independent 5x8 seat grid via two-dimensional Java arrays.
 * - Persisting seat availability ('AVAILABLE' and 'BOOKED').
 * - High-concurrency seat locking and transactional updates.
 */
public class SeatDAO {

    public static final int GRID_ROWS = 5; // Rows A through E
    public static final int GRID_COLS = 8; // Columns 1 through 8

    /**
     * Retrieves the live seat availability for a show as a 5x8 two-dimensional Java array.
     * Row index 0..4 corresponds to 'A'..'E'.
     * Column index 0..7 corresponds to 1..8.
     *
     * @param showId show identifier
     * @return 5x8 2D array of ShowSeat instances
     * @throws SQLException on database query error
     */
    public ShowSeat[][] getSeatGrid(int showId) throws SQLException {
        ShowSeat[][] grid = new ShowSeat[GRID_ROWS][GRID_COLS];
        List<ShowSeat> seats = getShowSeatsByShowId(showId);

        for (ShowSeat ss : seats) {
            String code = ss.getSeatCode();
            if (code != null && code.length() >= 2) {
                char rowChar = Character.toUpperCase(code.charAt(0));
                try {
                    int colNum = Integer.parseInt(code.substring(1).trim());
                    int r = rowChar - 'A';
                    int c = colNum - 1;
                    if (r >= 0 && r < GRID_ROWS && c >= 0 && c < GRID_COLS) {
                        grid[r][c] = ss;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore malformed codes outside standard grid
                }
            }
        }
        return grid;
    }

    /**
     * Retrieves the live seat availability as a 5x8 2D char array where:
     * 'O' = AVAILABLE
     * 'X' = BOOKED
     *
     * @param showId show identifier
     * @return 5x8 2D array of char status indicators
     * @throws SQLException on database query error
     */
    public char[][] getSeatStatusGrid(int showId) throws SQLException {
        char[][] statusGrid = new char[GRID_ROWS][GRID_COLS];
        ShowSeat[][] seatGrid = getSeatGrid(showId);

        for (int r = 0; r < GRID_ROWS; r++) {
            for (int c = 0; c < GRID_COLS; c++) {
                ShowSeat ss = seatGrid[r][c];
                if (ss != null && ss.isAvailable()) {
                    statusGrid[r][c] = 'O';
                } else {
                    statusGrid[r][c] = 'X';
                }
            }
        }
        return statusGrid;
    }

    /**
     * Retrieves all show_seats records for a given show, joined with physical seats metadata.
     *
     * @param showId show identifier
     * @return list of ShowSeat objects
     * @throws SQLException on database error
     */
    public List<ShowSeat> getShowSeatsByShowId(int showId) throws SQLException {
        List<ShowSeat> showSeats = new ArrayList<>();
        String sql = "SELECT ss.show_seat_id, ss.show_id, ss.seat_id, ss.status, "
                   + "       s.seat_code, s.seat_type "
                   + "FROM show_seats ss "
                   + "JOIN seats s ON ss.seat_id = s.seat_id "
                   + "WHERE ss.show_id = ? "
                   + "ORDER BY s.seat_code ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, showId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    showSeats.add(mapResultSetToShowSeat(rs));
                }
            }
        }
        return showSeats;
    }

    /**
     * Retrieves a single ShowSeat by showId and seatCode.
     *
     * @param showId   show identifier
     * @param seatCode seat code (e.g. "A1", "C4")
     * @return Optional containing ShowSeat if found
     * @throws SQLException on database error
     */
    public Optional<ShowSeat> getShowSeatByCode(int showId, String seatCode) throws SQLException {
        String sql = "SELECT ss.show_seat_id, ss.show_id, ss.seat_id, ss.status, "
                   + "       s.seat_code, s.seat_type "
                   + "FROM show_seats ss "
                   + "JOIN seats s ON ss.seat_id = s.seat_id "
                   + "WHERE ss.show_id = ? AND UPPER(s.seat_code) = UPPER(?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, showId);
            pstmt.setString(2, seatCode.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToShowSeat(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Queries and locks specific show seats for update within an active JDBC transaction.
     * Enforces row-level pessimistic locking via SELECT ... FOR UPDATE.
     *
     * @param conn      active JDBC transaction connection
     * @param showId    show identifier
     * @param seatCodes collection of seat codes
     * @return list of locked ShowSeat records
     * @throws SQLException on database error
     */
    public List<ShowSeat> getShowSeatsForUpdate(Connection conn, int showId, List<String> seatCodes) throws SQLException {
        if (seatCodes == null || seatCodes.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < seatCodes.size(); i++) {
            if (i > 0) placeholders.append(", ");
            placeholders.append("?");
        }

        String sql = "SELECT ss.show_seat_id, ss.show_id, ss.seat_id, ss.status, "
                   + "       s.seat_code, s.seat_type "
                   + "FROM show_seats ss "
                   + "JOIN seats s ON ss.seat_id = s.seat_id "
                   + "WHERE ss.show_id = ? AND UPPER(s.seat_code) IN (" + placeholders + ") "
                   + "FOR UPDATE";

        List<ShowSeat> lockedSeats = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, showId);
            for (int i = 0; i < seatCodes.size(); i++) {
                pstmt.setString(i + 2, seatCodes.get(i).trim().toUpperCase());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lockedSeats.add(mapResultSetToShowSeat(rs));
                }
            }
        }
        return lockedSeats;
    }

    /**
     * Updates the status of a show seat in an active transaction.
     *
     * @param conn       active JDBC transaction connection
     * @param showSeatId show seat identifier
     * @param status     new status ('AVAILABLE', 'BOOKED')
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean updateShowSeatStatus(Connection conn, int showSeatId, String status) throws SQLException {
        String sql = "UPDATE show_seats SET status = ? WHERE show_seat_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.trim().toUpperCase());
            pstmt.setInt(2, showSeatId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Standalone update of show seat status (auto-commit connection).
     *
     * @param showSeatId show seat identifier
     * @param status     new status
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean updateShowSeatStatus(int showSeatId, String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return updateShowSeatStatus(conn, showSeatId, status);
        }
    }

    /**
     * Batch updates status of multiple show seats within a transaction.
     * Useful for booking or cancellation.
     *
     * @param conn        active JDBC transaction connection
     * @param showSeatIds list of show seat IDs
     * @param status      new status ('AVAILABLE' or 'BOOKED')
     * @return total updated count
     * @throws SQLException on database error
     */
    public int updateShowSeatsStatusBatch(Connection conn, List<Integer> showSeatIds, String status) throws SQLException {
        if (showSeatIds == null || showSeatIds.isEmpty()) {
            return 0;
        }
        String sql = "UPDATE show_seats SET status = ? WHERE show_seat_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int id : showSeatIds) {
                pstmt.setString(1, status.trim().toUpperCase());
                pstmt.setInt(2, id);
                pstmt.addBatch();
            }
            int[] results = pstmt.executeBatch();
            int count = 0;
            for (int r : results) {
                if (r > 0) count += r;
            }
            return count;
        }
    }

    /**
     * Checks if a seat is currently available for a given show.
     *
     * @param showId   show identifier
     * @param seatCode seat code (e.g. "B4")
     * @return true if seat exists and status is 'AVAILABLE'
     * @throws SQLException on database error
     */
    public boolean isSeatAvailable(int showId, String seatCode) throws SQLException {
        Optional<ShowSeat> seatOpt = getShowSeatByCode(showId, seatCode);
        return seatOpt.isPresent() && seatOpt.get().isAvailable();
    }

    /**
     * Retrieves a physical seat record from 'seats' table by screen ID and seat code.
     *
     * @param screenId screen identifier
     * @param seatCode seat code
     * @return Optional containing physical Seat
     * @throws SQLException on database error
     */
    public Optional<Seat> getPhysicalSeat(int screenId, String seatCode) throws SQLException {
        String sql = "SELECT seat_id, screen_id, seat_code, seat_type FROM seats WHERE screen_id = ? AND UPPER(seat_code) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, screenId);
            pstmt.setString(2, seatCode.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Seat(
                            rs.getInt("seat_id"),
                            rs.getInt("screen_id"),
                            rs.getString("seat_code"),
                            rs.getString("seat_type")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    private ShowSeat mapResultSetToShowSeat(ResultSet rs) throws SQLException {
        ShowSeat ss = new ShowSeat(
                rs.getInt("show_seat_id"),
                rs.getInt("show_id"),
                rs.getInt("seat_id"),
                rs.getString("status")
        );
        ss.setSeatCode(rs.getString("seat_code"));
        ss.setSeatType(rs.getString("seat_type"));
        return ss;
    }
}
