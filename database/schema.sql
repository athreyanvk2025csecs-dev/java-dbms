-- =====================================================================
-- CineBook – Movie Ticket Booking System
-- Database Schema Script
-- Database: cinebook_db
-- Target DBMS: MySQL 8.0+
-- Member 1 Responsibility: Core Schema & Entity Tables
-- =====================================================================

CREATE DATABASE IF NOT EXISTS cinebook_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE cinebook_db;

-- Drop dependent tables in reverse order of foreign key dependencies
DROP TABLE IF EXISTS cancellations;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS booking_seats;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS show_seats;
DROP TABLE IF EXISTS shows;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS screens;
DROP TABLE IF EXISTS theatres;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS users;

-- ---------------------------------------------------------------------
-- 1. Table: users
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'CUSTOMER', 'STAFF'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. Table: movies
-- ---------------------------------------------------------------------
CREATE TABLE movies (
    movie_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    language VARCHAR(50) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    duration_minutes INT NOT NULL,
    release_date DATE NOT NULL,
    CONSTRAINT chk_duration CHECK (duration_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. Table: theatres
-- ---------------------------------------------------------------------
CREATE TABLE theatres (
    theatre_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    address VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 4. Table: screens
-- ---------------------------------------------------------------------
CREATE TABLE screens (
    screen_id INT AUTO_INCREMENT PRIMARY KEY,
    theatre_id INT NOT NULL,
    screen_name VARCHAR(50) NOT NULL,
    CONSTRAINT fk_screens_theatre
        FOREIGN KEY (theatre_id) REFERENCES theatres (theatre_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_theatre_screen UNIQUE (theatre_id, screen_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 5. Table: seats
-- ---------------------------------------------------------------------
CREATE TABLE seats (
    seat_id INT AUTO_INCREMENT PRIMARY KEY,
    screen_id INT NOT NULL,
    seat_code VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_seats_screen
        FOREIGN KEY (screen_id) REFERENCES screens (screen_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_screen_seat UNIQUE (screen_id, seat_code),
    CONSTRAINT chk_seat_type CHECK (seat_type IN ('REGULAR', 'PREMIUM', 'VIP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 6. Table: shows
-- ---------------------------------------------------------------------
CREATE TABLE shows (
    show_id INT AUTO_INCREMENT PRIMARY KEY,
    movie_id INT NOT NULL,
    screen_id INT NOT NULL,
    show_date DATE NOT NULL,
    show_time TIME NOT NULL,
    CONSTRAINT fk_shows_movie
        FOREIGN KEY (movie_id) REFERENCES movies (movie_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_shows_screen
        FOREIGN KEY (screen_id) REFERENCES screens (screen_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_screen_show_slot UNIQUE (screen_id, show_date, show_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 7. Table: show_seats
-- ---------------------------------------------------------------------
CREATE TABLE show_seats (
    show_seat_id INT AUTO_INCREMENT PRIMARY KEY,
    show_id INT NOT NULL,
    seat_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT fk_showseats_show
        FOREIGN KEY (show_id) REFERENCES shows (show_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_showseats_seat
        FOREIGN KEY (seat_id) REFERENCES seats (seat_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_show_seat UNIQUE (show_id, seat_id),
    CONSTRAINT chk_show_seat_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'LOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 8. Table: bookings
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    show_id INT NOT NULL,
    booking_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    final_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    CONSTRAINT fk_bookings_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_bookings_show
        FOREIGN KEY (show_id) REFERENCES shows (show_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_booking_status CHECK (status IN ('CONFIRMED', 'CANCELLED', 'PENDING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 9. Table: booking_seats
-- ---------------------------------------------------------------------
CREATE TABLE booking_seats (
    booking_seat_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    show_seat_id INT NOT NULL,
    seat_price DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_bookingseats_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_bookingseats_showseat
        FOREIGN KEY (show_seat_id) REFERENCES show_seats (show_seat_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_booking_showseat UNIQUE (show_seat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 10. Table: payments
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    amount DECIMAL(10,2) NOT NULL,
    paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('COMPLETED', 'FAILED', 'REFUNDED', 'PENDING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 11. Table: cancellations
-- ---------------------------------------------------------------------
CREATE TABLE cancellations (
    cancellation_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    cancelled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSED',
    CONSTRAINT fk_cancellations_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_cancellation_status CHECK (status IN ('PROCESSED', 'REJECTED', 'PENDING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
