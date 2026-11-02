package com.cinebook.dao;

import com.cinebook.database.DBConnection;
import com.cinebook.model.Movie;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for 'movies' table.
 * Supports adding, viewing, searching, updating, and deleting movie records.
 */
public class MovieDAO {

    /**
     * Adds a new movie to the database.
     *
     * @param movie Movie object to persist
     * @return generated movie_id
     * @throws SQLException on database error
     */
    public int addMovie(Movie movie) throws SQLException {
        String sql = "INSERT INTO movies (title, language, genre, duration_minutes, release_date) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, movie.getTitle());
            pstmt.setString(2, movie.getLanguage());
            pstmt.setString(3, movie.getGenre());
            pstmt.setInt(4, movie.getDurationMinutes());
            pstmt.setDate(5, Date.valueOf(movie.getReleaseDate()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Adding movie failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    movie.setMovieId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Adding movie failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Retrieves a movie by its primary key.
     *
     * @param movieId movie identifier
     * @return Optional containing Movie if found
     * @throws SQLException on database error
     */
    public Optional<Movie> getMovieById(int movieId) throws SQLException {
        String sql = "SELECT movie_id, title, language, genre, duration_minutes, release_date "
                   + "FROM movies WHERE movie_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMovie(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all movies ordered by release date descending.
     *
     * @return list of all movies
     * @throws SQLException on database error
     */
    public List<Movie> getAllMovies() throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT movie_id, title, language, genre, duration_minutes, release_date "
                   + "FROM movies ORDER BY release_date DESC, movie_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                movies.add(mapResultSetToMovie(rs));
            }
        }
        return movies;
    }

    /**
     * Searches movies by title (partial matching, case-insensitive).
     *
     * @param titleQuery query string
     * @return list of matching movies
     * @throws SQLException on database error
     */
    public List<Movie> searchMoviesByTitle(String titleQuery) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT movie_id, title, language, genre, duration_minutes, release_date "
                   + "FROM movies WHERE LOWER(title) LIKE LOWER(?) ORDER BY title ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + (titleQuery != null ? titleQuery.trim() : "") + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movies.add(mapResultSetToMovie(rs));
                }
            }
        }
        return movies;
    }

    /**
     * Updates an existing movie record.
     *
     * @param movie Movie entity with updated values
     * @return true if updated, false if movie not found
     * @throws SQLException on database error
     */
    public boolean updateMovie(Movie movie) throws SQLException {
        String sql = "UPDATE movies SET title = ?, language = ?, genre = ?, duration_minutes = ?, release_date = ? "
                   + "WHERE movie_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, movie.getTitle());
            pstmt.setString(2, movie.getLanguage());
            pstmt.setString(3, movie.getGenre());
            pstmt.setInt(4, movie.getDurationMinutes());
            pstmt.setDate(5, Date.valueOf(movie.getReleaseDate()));
            pstmt.setInt(6, movie.getMovieId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a movie by ID.
     *
     * @param movieId movie identifier
     * @return true if deleted, false otherwise
     * @throws SQLException on database error
     */
    public boolean deleteMovie(int movieId) throws SQLException {
        String sql = "DELETE FROM movies WHERE movie_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movieId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Movie mapResultSetToMovie(ResultSet rs) throws SQLException {
        Date releaseDate = rs.getDate("release_date");
        return new Movie(
                rs.getInt("movie_id"),
                rs.getString("title"),
                rs.getString("language"),
                rs.getString("genre"),
                rs.getInt("duration_minutes"),
                releaseDate != null ? releaseDate.toLocalDate() : null
        );
    }
}
