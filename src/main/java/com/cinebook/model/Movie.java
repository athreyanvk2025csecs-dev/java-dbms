package com.cinebook.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Model representing a Movie in CineBook.
 * Corresponds to table 'movies' (movie_id, title, language, genre, duration_minutes, release_date).
 */
public class Movie {

    private int movieId;
    private String title;
    private String language;
    private String genre;
    private int durationMinutes;
    private LocalDate releaseDate;

    public Movie() {
    }

    public Movie(String title, String language, String genre, int durationMinutes, LocalDate releaseDate) {
        this.title = title;
        this.language = language;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
        this.releaseDate = releaseDate;
    }

    public Movie(int movieId, String title, String language, String genre, int durationMinutes, LocalDate releaseDate) {
        this.movieId = movieId;
        this.title = title;
        this.language = language;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
        this.releaseDate = releaseDate;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return movieId == movie.movieId && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movieId, title);
    }

    @Override
    public String toString() {
        return String.format("Movie [ID: %d | Title: %s | Language: %s | Genre: %s | Duration: %d mins | Released: %s]",
                movieId, title, language, genre, durationMinutes, releaseDate != null ? releaseDate.toString() : "N/A");
    }
}
