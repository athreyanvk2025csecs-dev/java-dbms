package com.cinebook.service;

import com.cinebook.dao.MovieDAO;
import com.cinebook.dao.ScreenDAO;
import com.cinebook.dao.ShowDAO;
import com.cinebook.model.Movie;
import com.cinebook.model.Screen;
import com.cinebook.model.Show;
import com.cinebook.model.ShowSeat;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing movie show scheduling and independent seat availability.
 * Covers:
 * - Scheduling movie shows
 * - Generating show_seats records for each show
 * - Ensuring each show has an independent seat availability record
 * - Live seat map visualization (FR-2 requirement)
 */
public class ShowService {

    private final ShowDAO showDAO;
    private final MovieDAO movieDAO;
    private final ScreenDAO screenDAO;

    public ShowService() {
        this.showDAO = new ShowDAO();
        this.movieDAO = new MovieDAO();
        this.screenDAO = new ScreenDAO();
    }

    public ShowService(ShowDAO showDAO, MovieDAO movieDAO, ScreenDAO screenDAO) {
        this.showDAO = showDAO;
        this.movieDAO = movieDAO;
        this.screenDAO = screenDAO;
    }

    /**
     * Schedules a new movie show at a specific screen, date, and time.
     * Automatically generates independent show_seats availability records for this show.
     *
     * @param movieId   movie to screen
     * @param screenId  screen hosting the screening
     * @param showDate  date of screening
     * @param showTime  start time of screening
     * @return persisted Show entity with generated ID and enriched metadata
     * @throws IllegalArgumentException on validation failure
     * @throws SQLException             on database error
     */
    public Show scheduleShow(int movieId, int screenId, LocalDate showDate, LocalTime showTime) throws SQLException {
        if (showDate == null) {
            throw new IllegalArgumentException("Show date cannot be null.");
        }
        if (showTime == null) {
            throw new IllegalArgumentException("Show time cannot be null.");
        }

        // Verify movie existence
        Optional<Movie> movieOpt = movieDAO.getMovieById(movieId);
        if (movieOpt.isEmpty()) {
            throw new IllegalArgumentException("Movie with ID " + movieId + " does not exist.");
        }

        // Verify screen existence
        Optional<Screen> screenOpt = screenDAO.getScreenById(screenId);
        if (screenOpt.isEmpty()) {
            throw new IllegalArgumentException("Screen with ID " + screenId + " does not exist.");
        }

        Screen screen = screenOpt.get();
        if (screen.getSeats().isEmpty()) {
            // Provision default seats if screen has no physical seats yet
            screenDAO.setupDefaultScreenSeats(screenId);
        }

        Show show = new Show(movieId, screenId, showDate, showTime);
        int generatedId = showDAO.addShow(show);
        show.setShowId(generatedId);

        // Retrieve fully enriched show
        return showDAO.getShowById(generatedId).orElse(show);
    }

    /**
     * Retrieves show details by ID.
     *
     * @param showId show identifier
     * @return Optional containing enriched Show
     * @throws SQLException on database error
     */
    public Optional<Show> getShowById(int showId) throws SQLException {
        return showDAO.getShowById(showId);
    }

    /**
     * Retrieves all scheduled shows.
     *
     * @return list of scheduled shows
     * @throws SQLException on database error
     */
    public List<Show> getAllShows() throws SQLException {
        return showDAO.getAllShows();
    }

    /**
     * Retrieves all shows for a given movie.
     *
     * @param movieId movie identifier
     * @return list of shows
     * @throws SQLException on database error
     */
    public List<Show> getShowsByMovie(int movieId) throws SQLException {
        return showDAO.getShowsByMovieId(movieId);
    }

    /**
     * Retrieves all shows scheduled on a specific date.
     *
     * @param date target date
     * @return list of shows
     * @throws SQLException on database error
     */
    public List<Show> getShowsByDate(LocalDate date) throws SQLException {
        return showDAO.getShowsByDate(date);
    }

    /**
     * Returns the independent list of seat availability records for this specific show.
     *
     * @param showId show identifier
     * @return list of ShowSeat items
     * @throws SQLException on database error
     */
    public List<ShowSeat> getShowSeats(int showId) throws SQLException {
        return showDAO.getShowSeats(showId);
    }

    /**
     * Updates seat status for a show seat (e.g. BOOKED).
     *
     * @param showSeatId show seat identifier
     * @param status     new status ('AVAILABLE', 'BOOKED')
     * @return true if updated
     * @throws SQLException on database error
     */
    public boolean updateSeatStatus(int showSeatId, String status) throws SQLException {
        return showDAO.updateShowSeatStatus(showSeatId, status);
    }

    /**
     * Formats and prints a live 2D seat map for console display.
     * Marks 'O' for available seats, 'X' for booked seats.
     *
     * @param showId show identifier
     * @return string representation of the seat grid
     * @throws SQLException on database error
     */
    public String renderSeatMap(int showId) throws SQLException {
        List<ShowSeat> seats = showDAO.getShowSeats(showId);
        if (seats.isEmpty()) {
            return "No seat records found for Show #" + showId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%n================ SEAT AVAILABILITY MAP (Show #%d) ================\n", showId));
        sb.append("Legend: [O] = Available | [X] = Booked\n");
        sb.append("Tiers : Rows A-B = Regular (Rs.150) | Rows C-D = Premium (Rs.250) | Row E = VIP (Rs.400)\n\n");
        sb.append("      1   2   3   4   5   6   7   8\n");
        sb.append("    ---------------------------------\n");

        char currentRow = 0;
        for (ShowSeat ss : seats) {
            String code = ss.getSeatCode();
            if (code == null || code.length() < 2) continue;

            char rowChar = code.charAt(0);
            if (rowChar != currentRow) {
                if (currentRow != 0) {
                    sb.append("\n");
                }
                currentRow = rowChar;
                sb.append(" ").append(rowChar).append("  ");
            }

            String mark = ss.isAvailable() ? "[O]" : "[X]";
            sb.append(mark).append(" ");
        }
        sb.append("\n===================================================================\n");
        return sb.toString();
    }
}
