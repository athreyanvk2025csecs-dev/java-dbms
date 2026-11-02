package com.cinebook.dao;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Theatre;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for 'theatres' table.
 * Supports adding, viewing, filtering by city, updating, and deleting theatre records.
 */
public class TheatreDAO {

    /**
     * Adds a new theatre.
     *
     * @param theatre Theatre object to persist
     * @return generated theatre_id
     * @throws SQLException on database error
     */
    public int addTheatre(Theatre theatre) throws SQLException {
        String sql = "INSERT INTO theatres (name, city, address) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, theatre.getName());
            pstmt.setString(2, theatre.getCity());
            pstmt.setString(3, theatre.getAddress());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Adding theatre failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    theatre.setTheatreId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Adding theatre failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Retrieves a theatre by its primary key.
     *
     * @param theatreId theatre identifier
     * @return Optional containing Theatre if found
     * @throws SQLException on database error
     */
    public Optional<Theatre> getTheatreById(int theatreId) throws SQLException {
        String sql = "SELECT theatre_id, name, city, address FROM theatres WHERE theatre_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, theatreId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTheatre(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all theatres ordered by city and name.
     *
     * @return list of all theatres
     * @throws SQLException on database error
     */
    public List<Theatre> getAllTheatres() throws SQLException {
        List<Theatre> theatres = new ArrayList<>();
        String sql = "SELECT theatre_id, name, city, address FROM theatres ORDER BY city ASC, name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                theatres.add(mapResultSetToTheatre(rs));
            }
        }
        return theatres;
    }

    /**
     * Finds all theatres located in a specific city (case-insensitive).
     *
     * @param city city name
     * @return list of theatres in the city
     * @throws SQLException on database error
     */
    public List<Theatre> getTheatresByCity(String city) throws SQLException {
        List<Theatre> theatres = new ArrayList<>();
        String sql = "SELECT theatre_id, name, city, address FROM theatres WHERE LOWER(city) = LOWER(?) ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, city != null ? city.trim() : "");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    theatres.add(mapResultSetToTheatre(rs));
                }
            }
        }
        return theatres;
    }

    /**
     * Updates an existing theatre.
     *
     * @param theatre Theatre entity with updated data
     * @return true if updated, false if theatre not found
     * @throws SQLException on database error
     */
    public boolean updateTheatre(Theatre theatre) throws SQLException {
        String sql = "UPDATE theatres SET name = ?, city = ?, address = ? WHERE theatre_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, theatre.getName());
            pstmt.setString(2, theatre.getCity());
            pstmt.setString(3, theatre.getAddress());
            pstmt.setInt(4, theatre.getTheatreId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a theatre by ID (cascades to screens and shows).
     *
     * @param theatreId theatre identifier
     * @return true if deleted, false otherwise
     * @throws SQLException on database error
     */
    public boolean deleteTheatre(int theatreId) throws SQLException {
        String sql = "DELETE FROM theatres WHERE theatre_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, theatreId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Theatre mapResultSetToTheatre(ResultSet rs) throws SQLException {
        return new Theatre(
                rs.getInt("theatre_id"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("address")
        );
    }
}
