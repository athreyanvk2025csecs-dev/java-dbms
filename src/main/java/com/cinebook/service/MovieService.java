package com.cinebook.service;

import com.cinebook.dao.MovieDAO;
import com.cinebook.dao.ScreenDAO;
import com.cinebook.dao.TheatreDAO;
import com.cinebook.model.Movie;
import com.cinebook.model.Screen;
import com.cinebook.model.Seat;
import com.cinebook.model.Theatre;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing movies, theatres, screens, and seat layouts.
 * Covers:
 * - Adding and viewing movies
 * - Adding and viewing theatres
 * - Adding and viewing screens
 * - Associating each screen with its seats
 */
public class MovieService {

    private final MovieDAO movieDAO;
    private final TheatreDAO theatreDAO;
    private final ScreenDAO screenDAO;

    public MovieService() {
        this.movieDAO = new MovieDAO();
        this.theatreDAO = new TheatreDAO();
        this.screenDAO = new ScreenDAO();
    }

    public MovieService(MovieDAO movieDAO, TheatreDAO theatreDAO, ScreenDAO screenDAO) {
        this.movieDAO = movieDAO;
        this.theatreDAO = theatreDAO;
        this.screenDAO = screenDAO;
    }

    // -----------------------------------------------------------------
    // Movie Management
    // -----------------------------------------------------------------

    /**
     * Adds a new movie to CineBook catalog.
     *
     * @param title           movie title
     * @param language        spoken language
     * @param genre           movie genre
     * @param durationMinutes duration in minutes (> 0)
     * @param releaseDate     official release date
     * @return persisted Movie entity
     * @throws IllegalArgumentException on invalid parameters
     * @throws SQLException             on database error
     */
    public Movie addMovie(String title, String language, String genre, int durationMinutes, LocalDate releaseDate)
            throws SQLException {

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Movie title cannot be empty.");
        }
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("Movie language cannot be empty.");
        }
        if (genre == null || genre.trim().isEmpty()) {
            throw new IllegalArgumentException("Movie genre cannot be empty.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Movie duration must be greater than zero.");
        }
        if (releaseDate == null) {
            throw new IllegalArgumentException("Release date cannot be null.");
        }

        Movie movie = new Movie(title.trim(), language.trim(), genre.trim(), durationMinutes, releaseDate);
        int generatedId = movieDAO.addMovie(movie);
        movie.setMovieId(generatedId);
        return movie;
    }

    public List<Movie> getAllMovies() throws SQLException {
        return movieDAO.getAllMovies();
    }

    public Optional<Movie> getMovieById(int movieId) throws SQLException {
        return movieDAO.getMovieById(movieId);
    }

    public List<Movie> searchMovies(String query) throws SQLException {
        return movieDAO.searchMoviesByTitle(query);
    }

    // -----------------------------------------------------------------
    // Theatre Management
    // -----------------------------------------------------------------

    /**
     * Adds a new theatre.
     *
     * @param name    theatre name
     * @param city    city name
     * @param address physical address
     * @return persisted Theatre entity
     * @throws SQLException on database error
     */
    public Theatre addTheatre(String name, String city, String address) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Theatre name cannot be empty.");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("Theatre city cannot be empty.");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Theatre address cannot be empty.");
        }

        Theatre theatre = new Theatre(name.trim(), city.trim(), address.trim());
        int generatedId = theatreDAO.addTheatre(theatre);
        theatre.setTheatreId(generatedId);
        return theatre;
    }

    public List<Theatre> getAllTheatres() throws SQLException {
        return theatreDAO.getAllTheatres();
    }

    public Optional<Theatre> getTheatreById(int theatreId) throws SQLException {
        return theatreDAO.getTheatreById(theatreId);
    }

    public List<Theatre> getTheatresByCity(String city) throws SQLException {
        return theatreDAO.getTheatresByCity(city);
    }

    // -----------------------------------------------------------------
    // Screen & Seat Layout Management
    // -----------------------------------------------------------------

    /**
     * Adds a new screen under a theatre and automatically provisions a standard 40-seat layout
     * (Rows A-B Regular, C-D Premium, E VIP) satisfying screen-seat association requirements.
     *
     * @param theatreId  theatre identifier
     * @param screenName name of screen (e.g. 'Audi 1 (IMAX)')
     * @return persisted Screen entity with generated seats populated
     * @throws SQLException on database error
     */
    public Screen addScreen(int theatreId, String screenName) throws SQLException {
        if (screenName == null || screenName.trim().isEmpty()) {
            throw new IllegalArgumentException("Screen name cannot be empty.");
        }

        // Verify theatre exists
        if (theatreDAO.getTheatreById(theatreId).isEmpty()) {
            throw new IllegalArgumentException("Theatre with ID " + theatreId + " does not exist.");
        }

        Screen screen = new Screen(theatreId, screenName.trim());
        int screenId = screenDAO.addScreen(screen);
        screen.setScreenId(screenId);

        // Associate screen with default seat layout (40 seats)
        screenDAO.setupDefaultScreenSeats(screenId);
        screen.setSeats(screenDAO.getSeatsByScreenId(screenId));
        return screen;
    }

    /**
     * Adds a screen with custom seat configuration.
     *
     * @param theatreId  theatre identifier
     * @param screenName screen name
     * @param seats      custom seat list
     * @return persisted Screen entity
     * @throws SQLException on database error
     */
    public Screen addScreenWithCustomSeats(int theatreId, String screenName, List<Seat> seats) throws SQLException {
        if (screenName == null || screenName.trim().isEmpty()) {
            throw new IllegalArgumentException("Screen name cannot be empty.");
        }
        if (theatreDAO.getTheatreById(theatreId).isEmpty()) {
            throw new IllegalArgumentException("Theatre with ID " + theatreId + " does not exist.");
        }

        Screen screen = new Screen(theatreId, screenName.trim());
        int screenId = screenDAO.addScreen(screen);
        screen.setScreenId(screenId);

        if (seats != null && !seats.isEmpty()) {
            for (Seat seat : seats) {
                seat.setScreenId(screenId);
            }
            screenDAO.addSeatsBatch(seats);
        }
        screen.setSeats(screenDAO.getSeatsByScreenId(screenId));
        return screen;
    }

    public List<Screen> getScreensByTheatre(int theatreId) throws SQLException {
        return screenDAO.getScreensByTheatreId(theatreId);
    }

    public Optional<Screen> getScreenById(int screenId) throws SQLException {
        return screenDAO.getScreenById(screenId);
    }

    public List<Seat> getSeatsForScreen(int screenId) throws SQLException {
        return screenDAO.getSeatsByScreenId(screenId);
    }
}
