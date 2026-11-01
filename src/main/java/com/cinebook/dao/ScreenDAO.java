package com.cinebook.dao;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Screen;
import com.cinebook.model.Seat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for 'screens' and 'seats' tables.
 * Manages screen registration, querying, and association of screens with seats.
 */
public class ScreenDAO {

    /**
     * Adds a new screen under a theatre.
     *
     * @param screen Screen entity
     * @return generated screen_id
     * @throws SQLException on database error
     */
    public int addScreen(Screen screen) throws SQLException {
        String sql = "INSERT INTO screens (theatre_id, screen_name) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, screen.getTheatreId());
            pstmt.setString(2, screen.getScreenName());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Adding screen failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    screen.setScreenId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Adding screen failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Retrieves a screen by its screen_id including all its associated seats.
     *
     * @param screenId screen identifier
     * @return Optional containing Screen with seats populated
     * @throws SQLException on database error
     */
    public Optional<Screen> getScreenById(int screenId) throws SQLException {
        String sql = "SELECT screen_id, theatre_id, screen_name FROM screens WHERE screen_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, screenId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Screen screen = mapResultSetToScreen(rs);
                    screen.setSeats(getSeatsByScreenId(screenId));
                    return Optional.of(screen);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all screens for a specific theatre.
     *
     * @param theatreId theatre identifier
     * @return list of screens
     * @throws SQLException on database error
     */
    public List<Screen> getScreensByTheatreId(int theatreId) throws SQLException {
        List<Screen> screens = new ArrayList<>();
        String sql = "SELECT screen_id, theatre_id, screen_name FROM screens WHERE theatre_id = ? ORDER BY screen_name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, theatreId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Screen screen = mapResultSetToScreen(rs);
                    screen.setSeats(getSeatsByScreenId(screen.getScreenId()));
                    screens.add(screen);
                }
            }
        }
        return screens;
    }

    /**
     * Retrieves all screens across all theatres.
     *
     * @return list of all screens
     * @throws SQLException on database error
     */
    public List<Screen> getAllScreens() throws SQLException {
        List<Screen> screens = new ArrayList<>();
        String sql = "SELECT screen_id, theatre_id, screen_name FROM screens ORDER BY theatre_id ASC, screen_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                screens.add(mapResultSetToScreen(rs));
            }
        }
        return screens;
    }

    /**
     * Updates a screen's name.
     *
     * @param screen Screen entity with updated values
     * @return true if updated, false if screen not found
     * @throws SQLException on database error
     */
    public boolean updateScreen(Screen screen) throws SQLException {
        String sql = "UPDATE screens SET screen_name = ? WHERE screen_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, screen.getScreenName());
            pstmt.setInt(2, screen.getScreenId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a screen by ID.
     *
     * @param screenId screen identifier
     * @return true if deleted, false otherwise
     * @throws SQLException on database error
     */
    public boolean deleteScreen(int screenId) throws SQLException {
        String sql = "DELETE FROM screens WHERE screen_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, screenId);
            return pstmt.executeUpdate() > 0;
        }
    }

    // -----------------------------------------------------------------
    // Seat Management & Screen-Seat Association Methods
    // -----------------------------------------------------------------

    /**
     * Adds an individual seat to a screen.
     *
     * @param seat Seat entity
     * @return generated seat_id
     * @throws SQLException on database error
     */
    public int addSeat(Seat seat) throws SQLException {
        String sql = "INSERT INTO seats (screen_id, seat_code, seat_type) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, seat.getScreenId());
            pstmt.setString(2, seat.getSeatCode().toUpperCase().trim());
            pstmt.setString(3, seat.getSeatType().toUpperCase().trim());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Adding seat failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    seat.setSeatId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Adding seat failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Batch inserts a list of seats associated with a screen.
     * Uses JDBC batch operations for high performance.
     *
     * @param seats list of Seat entities
     * @throws SQLException on database error
     */
    public void addSeatsBatch(List<Seat> seats) throws SQLException {
        if (seats == null || seats.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO seats (screen_id, seat_code, seat_type) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try {
                for (Seat seat : seats) {
                    pstmt.setInt(1, seat.getScreenId());
                    pstmt.setString(2, seat.getSeatCode().toUpperCase().trim());
                    pstmt.setString(3, seat.getSeatType().toUpperCase().trim());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Retrieves all seats belonging to a specific screen.
     *
     * @param screenId screen identifier
     * @return list of Seat entities ordered by seat_code
     * @throws SQLException on database error
     */
    public List<Seat> getSeatsByScreenId(int screenId) throws SQLException {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT seat_id, screen_id, seat_code, seat_type FROM seats WHERE screen_id = ? ORDER BY seat_code ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, screenId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    seats.add(mapResultSetToSeat(rs));
                }
            }
        }
        return seats;
    }

    /**
     * Generates a standard 5x8 grid (40 seats) for a newly added screen:
     * Rows A-B: REGULAR (Rs. 150)
     * Rows C-D: PREMIUM (Rs. 250)
     * Row E:    VIP     (Rs. 400)
     *
     * @param screenId target screen identifier
     * @throws SQLException on database error
     */
    public void setupDefaultScreenSeats(int screenId) throws SQLException {
        List<Seat> seats = new ArrayList<>();
        char[] rows = {'A', 'B', 'C', 'D', 'E'};
        for (char row : rows) {
            String seatType;
            if (row == 'A' || row == 'B') {
                seatType = "REGULAR";
            } else if (row == 'C' || row == 'D') {
                seatType = "PREMIUM";
            } else {
                seatType = "VIP";
            }

            for (int col = 1; col <= 8; col++) {
                String seatCode = "" + row + col;
                seats.add(new Seat(screenId, seatCode, seatType));
            }
        }
        addSeatsBatch(seats);
    }

    private Screen mapResultSetToScreen(ResultSet rs) throws SQLException {
        return new Screen(
                rs.getInt("screen_id"),
                rs.getInt("theatre_id"),
                rs.getString("screen_name")
        );
    }

    private Seat mapResultSetToSeat(ResultSet rs) throws SQLException {
        return new Seat(
                rs.getInt("seat_id"),
                rs.getInt("screen_id"),
                rs.getString("seat_code"),
                rs.getString("seat_type")
        );
    }
}
