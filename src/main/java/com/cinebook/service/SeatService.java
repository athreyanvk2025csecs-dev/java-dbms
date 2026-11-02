package com.cinebook.service;

import com.cinebook.dao.SeatDAO;
import com.cinebook.model.SeatType;
import com.cinebook.model.ShowSeat;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Service managing seat validation, seat availability, and 5x8 seat map rendering.
 *
 * Member 2 Responsibility:
 * - Independent 5x8 seat grid per show using two-dimensional Java arrays.
 * - 'O' means AVAILABLE, 'X' means BOOKED.
 * - Seat codes must strictly range from A1 to E8.
 * - Rejection of invalid seat codes (e.g., Z9, A0, F1) and already-booked seats.
 * - Console seat map formatting.
 */
public class SeatService {

    private final SeatDAO seatDAO;

    public SeatService() {
        this.seatDAO = new SeatDAO();
    }

    public SeatService(SeatDAO seatDAO) {
        this.seatDAO = seatDAO;
    }

    /**
     * Retrieves the independent 5x8 seat grid for a show using a two-dimensional Java array.
     * Never uses lists of lists.
     *
     * @param showId show identifier
     * @return 5x8 2D array of ShowSeat instances
     * @throws SQLException on database error
     */
    public ShowSeat[][] getSeatGrid(int showId) throws SQLException {
        return seatDAO.getSeatGrid(showId);
    }

    /**
     * Retrieves the 5x8 character availability grid for a show.
     * 'O' = AVAILABLE
     * 'X' = BOOKED
     *
     * @param showId show identifier
     * @return 5x8 2D char array
     * @throws SQLException on database error
     */
    public char[][] getSeatStatusGrid(int showId) throws SQLException {
        return seatDAO.getSeatStatusGrid(showId);
    }

    /**
     * Formats and returns the console seat map for a show.
     * Iterates through the required 5x8 two-dimensional Java array.
     *
     * @param showId show identifier
     * @return formatted string representing the seat map
     * @throws SQLException on database error
     */
    public String renderSeatMap(int showId) throws SQLException {
        char[][] statusGrid = getSeatStatusGrid(showId);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%n===================== SEAT MAP (Show #%d) =====================%n", showId));
        sb.append("                       [ SCREEN THIS WAY ]                       \n");
        sb.append("-----------------------------------------------------------------\n");
        sb.append("       1     2     3     4     5     6     7     8\n");

        char[] rowLabels = {'A', 'B', 'C', 'D', 'E'};
        for (int r = 0; r < SeatDAO.GRID_ROWS; r++) {
            char rowChar = rowLabels[r];
            SeatType tier = SeatType.fromRow(rowChar);
            sb.append("  ").append(rowChar).append("   ");

            for (int c = 0; c < SeatDAO.GRID_COLS; c++) {
                char statusChar = statusGrid[r][c];
                sb.append("[").append(statusChar).append("]  ");
            }

            sb.append(String.format(Locale.ROOT, " (%s - Rs.%.0f)%n", tier.name(), tier.getPrice()));
        }

        sb.append("-----------------------------------------------------------------\n");
        sb.append("Legend: [O] = Available   [X] = Booked\n");
        sb.append("Tiers : Rows A-B = Regular (Rs.150) | Rows C-D = Premium (Rs.250) | Row E = VIP (Rs.400)\n");
        sb.append("=================================================================\n");

        return sb.toString();
    }

    /**
     * Prints the formatted seat map directly to the console.
     *
     * @param showId show identifier
     * @throws SQLException on database error
     */
    public void displaySeatMap(int showId) throws SQLException {
        System.out.println(renderSeatMap(showId));
    }

    /**
     * Validates whether a seat code follows the CineBook 5x8 grid convention (A1 to E8).
     *
     * @param seatCode seat code to check (e.g., "A1", "C4", "E8")
     * @return true if seat code is between A1 and E8, false otherwise (e.g. "Z9", "A9")
     */
    public boolean isValidSeatCode(String seatCode) {
        if (seatCode == null) {
            return false;
        }
        String trimmed = seatCode.trim().toUpperCase();
        if (trimmed.length() != 2) {
            return false;
        }
        char row = trimmed.charAt(0);
        char col = trimmed.charAt(1);

        boolean validRow = (row >= 'A' && row <= 'E');
        boolean validCol = (col >= '1' && col <= '8');

        return validRow && validCol;
    }

    /**
     * Validates a seat code and throws IllegalArgumentException if invalid.
     *
     * @param seatCode seat code
     * @throws IllegalArgumentException if seat code is not A1-E8
     */
    public void validateSeatCode(String seatCode) {
        if (!isValidSeatCode(seatCode)) {
            throw new IllegalArgumentException(
                    "Invalid seat code: '" + seatCode + "'. Seat codes must be between A1 and E8."
            );
        }
    }

    /**
     * Validates a collection of selected seat codes for a show.
     * Checks:
     * 1. List is non-empty.
     * 2. No duplicate seat codes in the request.
     * 3. Every seat code is syntactically valid (A1 to E8).
     * 4. Every seat exists in the show.
     * 5. Every seat is currently AVAILABLE.
     *
     * If even one seat fails, throws an appropriate exception.
     *
     * @param showId    show identifier
     * @param seatCodes list of requested seat codes
     * @throws IllegalArgumentException on syntax error or duplicates
     * @throws IllegalStateException    if any seat is already booked
     * @throws SQLException             on database error
     */
    public void validateSeatsForBooking(int showId, List<String> seatCodes) throws SQLException {
        if (seatCodes == null || seatCodes.isEmpty()) {
            throw new IllegalArgumentException("No seats selected. Please choose at least one seat.");
        }

        Set<String> uniqueCodes = new HashSet<>();
        for (String code : seatCodes) {
            if (code == null) {
                throw new IllegalArgumentException("Seat code cannot be null.");
            }
            String upper = code.trim().toUpperCase();
            validateSeatCode(upper);

            if (!uniqueCodes.add(upper)) {
                throw new IllegalArgumentException("Duplicate seat code in selection: '" + upper + "'.");
            }
        }

        // Validate availability of every seat in database
        for (String code : seatCodes) {
            String upper = code.trim().toUpperCase();
            Optional<ShowSeat> ssOpt = seatDAO.getShowSeatByCode(showId, upper);
            if (ssOpt.isEmpty()) {
                throw new IllegalArgumentException("Seat '" + upper + "' does not exist for Show #" + showId + ".");
            }
            ShowSeat ss = ssOpt.get();
            if (!ss.isAvailable()) {
                throw new IllegalStateException("Seat '" + upper + "' is already booked for Show #" + showId + ".");
            }
        }
    }

    /**
     * Checks if a seat is currently available for a given show.
     *
     * @param showId   show identifier
     * @param seatCode seat code (e.g. "C3")
     * @return true if available
     * @throws SQLException on database error
     */
    public boolean isSeatAvailable(int showId, String seatCode) throws SQLException {
        if (!isValidSeatCode(seatCode)) {
            return false;
        }
        return seatDAO.isSeatAvailable(showId, seatCode);
    }

    /**
     * Retrieves a ShowSeat for a show by its seat code.
     *
     * @param showId   show identifier
     * @param seatCode seat code (e.g. "B2")
     * @return Optional containing ShowSeat
     * @throws SQLException on database error
     */
    public Optional<ShowSeat> getShowSeat(int showId, String seatCode) throws SQLException {
        validateSeatCode(seatCode);
        return seatDAO.getShowSeatByCode(showId, seatCode);
    }

    /**
     * Retrieves all show seats for a given show.
     *
     * @param showId show identifier
     * @return list of ShowSeat items
     * @throws SQLException on database error
     */
    public List<ShowSeat> getShowSeats(int showId) throws SQLException {
        return seatDAO.getShowSeatsByShowId(showId);
    }

    /**
     * Returns the pricing tier (SeatType) for a given seat code.
     *
     * @param seatCode seat code
     * @return SeatType enum
     */
    public SeatType getSeatTypeForCode(String seatCode) {
        validateSeatCode(seatCode);
        return SeatType.fromSeatCode(seatCode);
    }

    /**
     * Returns the price for a given seat code based on SeatType.
     *
     * @param seatCode seat code
     * @return seat price in INR
     */
    public double getPriceForSeat(String seatCode) {
        return getSeatTypeForCode(seatCode).getPrice();
    }
}
