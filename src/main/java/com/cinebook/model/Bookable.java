package com.cinebook.model;

import java.util.List;

/**
 * Shared contract interface for bookable entities in CineBook.
 * Implemented by Show as specified in the CineBook project specifications.
 */
public interface Bookable {

    /**
     * Attempts to book seats for this bookable entity.
     *
     * @return true if booking succeeded, false otherwise
     */
    boolean bookSeats();

    /**
     * Cancels a booking associated with this entity.
     *
     * @return true if cancellation succeeded, false otherwise
     */
    boolean cancelBooking();

    /**
     * Overloaded convenience method to book specific seat codes.
     *
     * @param seatCodes list of seat codes to book
     * @return true if booking succeeded, false otherwise
     */
    default boolean bookSeats(List<String> seatCodes) {
        return bookSeats();
    }

    /**
     * Overloaded convenience method to cancel a booking by ID.
     *
     * @param bookingId booking identifier
     * @return true if cancellation succeeded, false otherwise
     */
    default boolean cancelBooking(int bookingId) {
        return cancelBooking();
    }

    /**
     * Overloaded convenience method to cancel a booking by booking code.
     *
     * @param bookingCode booking code string
     * @return true if cancellation succeeded, false otherwise
     */
    default boolean cancelBooking(String bookingCode) {
        return cancelBooking();
    }
}
