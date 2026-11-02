package com.cinebook.model;

/**
 * Enum representing seat tiers and their respective pricing in CineBook.
 * - Rows A and B = REGULAR (Rs. 150)
 * - Rows C and D = PREMIUM (Rs. 250)
 * - Row E        = VIP     (Rs. 400)
 */
public enum SeatType {
    REGULAR(150.0),
    PREMIUM(250.0),
    VIP(400.0);

    private final double price;

    SeatType(double price) {
        this.price = price;
    }

    /**
     * Returns the base ticket price for this seat tier.
     *
     * @return seat price in INR
     */
    public double getPrice() {
        return price;
    }

    /**
     * Resolves the SeatType based on the seat row letter.
     * Rows A and B = REGULAR
     * Rows C and D = PREMIUM
     * Row E        = VIP
     *
     * @param row row letter ('A' through 'E')
     * @return corresponding SeatType
     * @throws IllegalArgumentException if row is not within A-E
     */
    public static SeatType fromRow(char row) {
        char upper = Character.toUpperCase(row);
        switch (upper) {
            case 'A':
            case 'B':
                return REGULAR;
            case 'C':
            case 'D':
                return PREMIUM;
            case 'E':
                return VIP;
            default:
                throw new IllegalArgumentException("Invalid seat row: '" + row + "'. Valid rows are A, B, C, D, and E.");
        }
    }

    /**
     * Resolves the SeatType based on a seat code (e.g., "A1", "C4", "E8").
     *
     * @param seatCode seat code string
     * @return corresponding SeatType
     * @throws IllegalArgumentException if seatCode is invalid
     */
    public static SeatType fromSeatCode(String seatCode) {
        if (seatCode == null || seatCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat code cannot be null or empty.");
        }
        return fromRow(seatCode.trim().charAt(0));
    }

    /**
     * Resolves the price for a given seat code using this enum.
     *
     * @param seatCode seat code string
     * @return seat price from SeatType
     */
    public static double getPriceForCode(String seatCode) {
        return fromSeatCode(seatCode).getPrice();
    }

    /**
     * Parses a string representation of SeatType (e.g. "REGULAR", "PREMIUM", "VIP").
     *
     * @param name seat type name
     * @return matching SeatType
     */
    public static SeatType fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat type name cannot be null or empty.");
        }
        return SeatType.valueOf(name.trim().toUpperCase());
    }
}
