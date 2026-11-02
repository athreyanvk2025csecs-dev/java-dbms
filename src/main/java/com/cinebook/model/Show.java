package com.cinebook.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a Movie Show (screening) in CineBook.
 * Corresponds to table 'shows' (show_id, movie_id, screen_id, show_date, show_time).
 * Implements Bookable interface as required by the CineBook shared contract.
 */
public class Show implements Bookable {

    private int showId;
    private int movieId;
    private int screenId;
    private LocalDate showDate;
    private LocalTime showTime;

    // Supplementary join/display fields for convenient UI/console display
    private String movieTitle;
    private String movieLanguage;
    private String screenName;
    private String theatreName;
    private String theatreCity;

    public Show() {
    }

    public Show(int movieId, int screenId, LocalDate showDate, LocalTime showTime) {
        this.movieId = movieId;
        this.screenId = screenId;
        this.showDate = showDate;
        this.showTime = showTime;
    }

    public Show(int showId, int movieId, int screenId, LocalDate showDate, LocalTime showTime) {
        this.showId = showId;
        this.movieId = movieId;
        this.screenId = screenId;
        this.showDate = showDate;
        this.showTime = showTime;
    }

    public int getShowId() {
        return showId;
    }

    public void setShowId(int showId) {
        this.showId = showId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getScreenId() {
        return screenId;
    }

    public void setScreenId(int screenId) {
        this.screenId = screenId;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public LocalTime getShowTime() {
        return showTime;
    }

    public void setShowTime(LocalTime showTime) {
        this.showTime = showTime;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMovieLanguage() {
        return movieLanguage;
    }

    public void setMovieLanguage(String movieLanguage) {
        this.movieLanguage = movieLanguage;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public void setTheatreName(String theatreName) {
        this.theatreName = theatreName;
    }

    public String getTheatreCity() {
        return theatreCity;
    }

    public void setTheatreCity(String theatreCity) {
        this.theatreCity = theatreCity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Show show = (Show) o;
        return showId == show.showId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(showId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Show [ID: %d | Date: %s | Time: %s", showId, showDate, showTime));
        if (movieTitle != null) {
            sb.append(" | Movie: ").append(movieTitle);
        }
        if (theatreName != null && screenName != null) {
            sb.append(" | Venue: ").append(theatreName).append(" - ").append(screenName);
        }
        sb.append("]");
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Bookable Implementation (Shared Contract with Member 2 & 3)
    // -----------------------------------------------------------------

    @Override
    public boolean bookSeats() {
        return true;
    }

    @Override
    public boolean cancelBooking() {
        return true;
    }

    @Override
    public boolean bookSeats(List<String> seatCodes) {
        return seatCodes != null && !seatCodes.isEmpty();
    }

    @Override
    public boolean cancelBooking(int bookingId) {
        return bookingId > 0;
    }

    @Override
    public boolean cancelBooking(String bookingCode) {
        return bookingCode != null && !bookingCode.trim().isEmpty();
    }
}
