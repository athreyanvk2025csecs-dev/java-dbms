package com.cinebook.dao;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Show;
import com.cinebook.model.ShowSeat;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for 'shows' and 'show_seats' tables.
 * Handles show scheduling, independent seat generation per show, and seat availability tracking.
 */
public class ShowDAO {

    /**
     * Schedules a new show and generates independent seat availability records.
     *
     * @param show Show entity containing movie, screen, date, and time
     * @return generated show_id
     * @throws SQLException on database error or conflicting schedule
     */
    public int addShow(Show show) throws SQLException {
        String sql = "INSERT INTO shows (movie_id, screen_id, show_date, show_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, show.getMovieId());
            pstmt.setInt(2, show.getScreenId());
            pstmt.setDate(3, Date.valueOf(show.getShowDate()));
            pstmt.setTime(4, Time.valueOf(show.getShowTime()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Scheduling show failed, no rows affected.");
            }

            int generatedId;
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    generatedId = generatedKeys.getInt(1);
                    show.setShowId(generatedId);
                } else {
                    throw new SQLException("Scheduling show failed, no ID obtained.");
                }
            }

            // Immediately generate independent seat availability records for this show
            generateShowSeats(generatedId, show.getScreenId());
            return generatedId;
        }
    }

    /**
     * Generates independent show_seats records for a newly scheduled show.
     * Copies all physical seats configured for the screen into show_seats with 'AVAILABLE' status.
     *
     * @param showId   newly created show identifier
     * @param screenId screen hosting the show
     * @return count of generated show_seat records
     * @throws SQLException on database error
     */
    public int generateShowSeats(int showId, int screenId) throws SQLException {
        String sql = "INSERT INTO show_seats (show_id, seat_id, status) "
                   + "SELECT ?, seat_id, 'AVAILABLE' FROM seats WHERE screen_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, showId);
            pstmt.setInt(2, screenId);
            return pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves a show by ID with joined movie, screen, and theatre names.
     *
     * @param showId show identifier
     * @return Optional containing enriched Show
     * @throws SQLException on database error
     */
    public Optional<Show> getShowById(int showId) throws SQLException {
        String sql = "SELECT s.show_id, s.movie_id, s.screen_id, s.show_date, s.show_time, "
                   + "       m.title AS movie_title, m.language AS movie_language, "
                   + "       sc.screen_name, t.name AS theatre_name, t.city AS theatre_city "
                   + "FROM shows s "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN screens sc ON s.screen_id = sc.screen_id "
                   + "JOIN theatres t ON sc.theatre_id = t.theatre_id "
                   + "WHERE s.show_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, showId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEnrichedShow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all scheduled shows across all theatres and movies.
     *
     * @return list of enriched Shows
     * @throws SQLException on database error
     */
    public List<Show> getAllShows() throws SQLException {
        List<Show> shows = new ArrayList<>();
        String sql = "SELECT s.show_id, s.movie_id, s.screen_id, s.show_date, s.show_time, "
                   + "       m.title AS movie_title, m.language AS movie_language, "
                   + "       sc.screen_name, t.name AS theatre_name, t.city AS theatre_city "
                   + "FROM shows s "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN screens sc ON s.screen_id = sc.screen_id "
                   + "JOIN theatres t ON sc.theatre_id = t.theatre_id "
                   + "ORDER BY s.show_date ASC, s.show_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                shows.add(mapResultSetToEnrichedShow(rs));
            }
        }
        return shows;
    }

    /**
     * Retrieves all shows scheduled for a particular movie.
     *
     * @param movieId movie identifier
     * @return list of shows for this movie
     * @throws SQLException on database error
     */
    public List<Show> getShowsByMovieId(int movieId) throws SQLException {
        List<Show> shows = new ArrayList<>();
        String sql = "SELECT s.show_id, s.movie_id, s.screen_id, s.show_date, s.show_time, "
                   + "       m.title AS movie_title, m.language AS movie_language, "
                   + "       sc.screen_name, t.name AS theatre_name, t.city AS theatre_city "
                   + "FROM shows s "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN screens sc ON s.screen_id = sc.screen_id "
                   + "JOIN theatres t ON sc.theatre_id = t.theatre_id "
                   + "WHERE s.movie_id = ? "
                   + "ORDER BY s.show_date ASC, s.show_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    shows.add(mapResultSetToEnrichedShow(rs));
                }
            }
        }
        return shows;
    }

    /**
     * Retrieves all shows scheduled on a particular date.
     *
     * @param showDate target date
     * @return list of shows scheduled for that date
     * @throws SQLException on database error
     */
    public List<Show> getShowsByDate(LocalDate showDate) throws SQLException {
        List<Show> shows = new ArrayList<>();
        String sql = "SELECT s.show_id, s.movie_id, s.screen_id, s.show_date, s.show_time, "
                   + "       m.title AS movie_title, m.language AS movie_language, "
                   + "       sc.screen_name, t.name AS theatre_name, t.city AS theatre_city "
                   + "FROM shows s "
                   + "JOIN movies m ON s.movie_id = m.movie_id "
                   + "JOIN screens sc ON s.screen_id = sc.screen_id "
                   + "JOIN theatres t ON sc.theatre_id = t.theatre_id "
                   + "WHERE s.show_date = ? "
                   + "ORDER BY s.show_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, Date.valueOf(showDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    shows.add(mapResultSetToEnrichedShow(rs));
                }
            }
        }
        return shows;
    }

    /**
     * Retrieves the independent seat availability map for a specific show.
     * Joins with physical seats table to retrieve seat code and seat type.
     *
     * @param showId show identifier
     * @return list of ShowSeat objects containing live status ('AVAILABLE', 'BOOKED')
     * @throws SQLException on database error
     */
    public List<ShowSeat> getShowSeats(int showId) throws SQLException {
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
                    ShowSeat showSeat = new ShowSeat(
                            rs.getInt("show_seat_id"),
                            rs.getInt("show_id"),
                            rs.getInt("seat_id"),
                            rs.getString("status")
                    );
                    showSeat.setSeatCode(rs.getString("seat_code"));
                    showSeat.setSeatType(rs.getString("seat_type"));
                    showSeats.add(showSeat);
                }
            }
        }
        return showSeats;
    }

    /**
     * Updates seat status for a show seat (e.g. from AVAILABLE to BOOKED or vice-versa).
     *
     * @param showSeatId identifier in show_seats table
     * @param status     new status ('AVAILABLE', 'BOOKED', 'LOCKED')
     * @return true if updated, false otherwise
     * @throws SQLException on database error
     */
    public boolean updateShowSeatStatus(int showSeatId, String status) throws SQLException {
        String sql = "UPDATE show_seats SET status = ? WHERE show_seat_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.toUpperCase().trim());
            pstmt.setInt(2, showSeatId);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates an existing show's date or time.
     *
     * @param show Show entity with updated schedule
     * @return true if updated, false if show not found
     * @throws SQLException on database error
     */
    public boolean updateShow(Show show) throws SQLException {
        String sql = "UPDATE shows SET movie_id = ?, screen_id = ?, show_date = ?, show_time = ? "
                   + "WHERE show_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, show.getMovieId());
            pstmt.setInt(2, show.getScreenId());
            pstmt.setDate(3, Date.valueOf(show.getShowDate()));
            pstmt.setTime(4, Time.valueOf(show.getShowTime()));
            pstmt.setInt(5, show.getShowId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a show by ID (cascades to show_seats).
     *
     * @param showId show identifier
     * @return true if deleted, false otherwise
     * @throws SQLException on database error
     */
    public boolean deleteShow(int showId) throws SQLException {
        String sql = "DELETE FROM shows WHERE show_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, showId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Show mapResultSetToEnrichedShow(ResultSet rs) throws SQLException {
        Date showDate = rs.getDate("show_date");
        Time showTime = rs.getTime("show_time");
        Show show = new Show(
                rs.getInt("show_id"),
                rs.getInt("movie_id"),
                rs.getInt("screen_id"),
                showDate != null ? showDate.toLocalDate() : null,
                showTime != null ? showTime.toLocalTime() : null
        );
        show.setMovieTitle(rs.getString("movie_title"));
        show.setMovieLanguage(rs.getString("movie_language"));
        show.setScreenName(rs.getString("screen_name"));
        show.setTheatreName(rs.getString("theatre_name"));
        show.setTheatreCity(rs.getString("theatre_city"));
        return show;
    }
}
