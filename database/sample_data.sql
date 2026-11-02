-- =====================================================================
-- CineBook – Movie Ticket Booking System
-- Database Sample Data Script
-- Database: cinebook_db
-- Member 1 Deliverable: Initial Test & Demonstration Records
-- =====================================================================

USE cinebook_db;

-- ---------------------------------------------------------------------
-- 1. Sample Users
-- Passwords hashed using SHA-256:
-- 'Admin@123'    -> 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
-- 'User@123'     -> 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
-- 'Rahul@123'    -> ef797c8118f02dfb649607dd5d3f8c7623048c9c063d532cc95c5ed7a898a64f
-- ---------------------------------------------------------------------
INSERT INTO users (user_id, full_name, email, password_hash, role) VALUES
(1, 'Administrator', 'admin@cinebook.com', SHA2('Admin@123', 256), 'ADMIN'),
(2, 'Athreyan V', 'athreyan@gmail.com', SHA2('User@123', 256), 'CUSTOMER'),
(3, 'Rahul Sharma', 'rahul.sharma@gmail.com', SHA2('Rahul@123', 256), 'CUSTOMER'),
(4, 'Priya Mehta', 'priya.mehta@gmail.com', SHA2('User@123', 256), 'CUSTOMER');

-- ---------------------------------------------------------------------
-- 2. Sample Movies
-- ---------------------------------------------------------------------
INSERT INTO movies (movie_id, title, language, genre, duration_minutes, release_date) VALUES
(1, 'Interstellar', 'English', 'Sci-Fi / Adventure', 169, '2014-11-07'),
(2, 'Inception', 'English', 'Action / Sci-Fi', 148, '2010-07-16'),
(3, 'The Dark Knight', 'English', 'Action / Crime', 152, '2008-07-18'),
(4, 'Amaran', 'Tamil', 'Action / Biography', 167, '2024-10-31'),
(5, 'Leo', 'Tamil', 'Action / Thriller', 164, '2023-10-19');

-- ---------------------------------------------------------------------
-- 3. Sample Theatres
-- ---------------------------------------------------------------------
INSERT INTO theatres (theatre_id, name, city, address) VALUES
(1, 'PVR Grand Mall', 'Chennai', 'Velachery Main Road, Chennai, Tamil Nadu 600042'),
(2, 'SPI Palazzo', 'Chennai', 'Nexus Vijaya Mall, Vadapalani, Chennai, Tamil Nadu 600026'),
(3, 'INOX Megaplex', 'Mumbai', 'Inorbit Mall, Malad West, Mumbai, Maharashtra 400064');

-- ---------------------------------------------------------------------
-- 4. Sample Screens
-- ---------------------------------------------------------------------
INSERT INTO screens (screen_id, theatre_id, screen_name) VALUES
(1, 1, 'Audi 1 (IMAX)'),
(2, 1, 'Audi 2 (Dolby Atmos)'),
(3, 2, 'Screen 1 (Palazzo Luxe)'),
(4, 3, 'Screen 1 (Laser 4K)');

-- ---------------------------------------------------------------------
-- 5. Sample Seats for Screen 1 (5 rows x 8 seats = 40 seats)
-- Row A-B: Regular (Rs. 150.00)
-- Row C-D: Premium (Rs. 250.00)
-- Row E:   VIP     (Rs. 400.00)
-- ---------------------------------------------------------------------
INSERT INTO seats (screen_id, seat_code, seat_type) VALUES
-- Row A (Regular)
(1, 'A1', 'REGULAR'), (1, 'A2', 'REGULAR'), (1, 'A3', 'REGULAR'), (1, 'A4', 'REGULAR'),
(1, 'A5', 'REGULAR'), (1, 'A6', 'REGULAR'), (1, 'A7', 'REGULAR'), (1, 'A8', 'REGULAR'),
-- Row B (Regular)
(1, 'B1', 'REGULAR'), (1, 'B2', 'REGULAR'), (1, 'B3', 'REGULAR'), (1, 'B4', 'REGULAR'),
(1, 'B5', 'REGULAR'), (1, 'B6', 'REGULAR'), (1, 'B7', 'REGULAR'), (1, 'B8', 'REGULAR'),
-- Row C (Premium)
(1, 'C1', 'PREMIUM'), (1, 'C2', 'PREMIUM'), (1, 'C3', 'PREMIUM'), (1, 'C4', 'PREMIUM'),
(1, 'C5', 'PREMIUM'), (1, 'C6', 'PREMIUM'), (1, 'C7', 'PREMIUM'), (1, 'C8', 'PREMIUM'),
-- Row D (Premium)
(1, 'D1', 'PREMIUM'), (1, 'D2', 'PREMIUM'), (1, 'D3', 'PREMIUM'), (1, 'D4', 'PREMIUM'),
(1, 'D5', 'PREMIUM'), (1, 'D6', 'PREMIUM'), (1, 'D7', 'PREMIUM'), (1, 'D8', 'PREMIUM'),
-- Row E (VIP)
(1, 'E1', 'VIP'), (1, 'E2', 'VIP'), (1, 'E3', 'VIP'), (1, 'E4', 'VIP'),
(1, 'E5', 'VIP'), (1, 'E6', 'VIP'), (1, 'E7', 'VIP'), (1, 'E8', 'VIP');

-- Also add sample seats for Screen 2
INSERT INTO seats (screen_id, seat_code, seat_type) VALUES
(2, 'A1', 'REGULAR'), (2, 'A2', 'REGULAR'), (2, 'A3', 'REGULAR'), (2, 'A4', 'REGULAR'),
(2, 'B1', 'PREMIUM'), (2, 'B2', 'PREMIUM'), (2, 'B3', 'PREMIUM'), (2, 'B4', 'PREMIUM'),
(2, 'C1', 'VIP'),     (2, 'C2', 'VIP'),     (2, 'C3', 'VIP'),     (2, 'C4', 'VIP');

-- ---------------------------------------------------------------------
-- 6. Sample Shows
-- ---------------------------------------------------------------------
INSERT INTO shows (show_id, movie_id, screen_id, show_date, show_time) VALUES
(1, 1, 1, '2026-11-05', '10:00:00'),
(2, 1, 1, '2026-11-05', '14:30:00'),
(3, 4, 1, '2026-11-05', '19:00:00'),
(4, 2, 2, '2026-11-05', '11:15:00');

-- ---------------------------------------------------------------------
-- 7. Sample Show Seats for Show 1 (Screen 1 seats generated for Show 1)
-- ---------------------------------------------------------------------
INSERT INTO show_seats (show_id, seat_id, status)
SELECT 1, seat_id, 'AVAILABLE' FROM seats WHERE screen_id = 1;

-- Generate for Show 2
INSERT INTO show_seats (show_id, seat_id, status)
SELECT 2, seat_id, 'AVAILABLE' FROM seats WHERE screen_id = 1;

-- Generate for Show 3
INSERT INTO show_seats (show_id, seat_id, status)
SELECT 3, seat_id, 'AVAILABLE' FROM seats WHERE screen_id = 1;

-- Generate for Show 4
INSERT INTO show_seats (show_id, seat_id, status)
SELECT 4, seat_id, 'AVAILABLE' FROM seats WHERE screen_id = 2;

-- Mark a couple of seats as booked in Show 1 for testing
UPDATE show_seats 
SET status = 'BOOKED' 
WHERE show_id = 1 AND seat_id IN (
    SELECT seat_id FROM seats WHERE screen_id = 1 AND seat_code IN ('C3', 'C4')
);

-- ---------------------------------------------------------------------
-- 8. Sample Bookings
-- ---------------------------------------------------------------------
INSERT INTO bookings (booking_id, booking_code, user_id, show_id, booking_datetime, total_amount, discount_amount, final_amount, status) VALUES
(1, 'BK-20261105-1001', 2, 1, '2026-11-02 11:30:00', 500.00, 50.00, 450.00, 'CONFIRMED');

-- ---------------------------------------------------------------------
-- 9. Sample Booking Seats
-- ---------------------------------------------------------------------
INSERT INTO booking_seats (booking_id, show_seat_id, seat_price)
SELECT 1, ss.show_seat_id, 250.00
FROM show_seats ss
JOIN seats s ON ss.seat_id = s.seat_id
WHERE ss.show_id = 1 AND s.seat_code IN ('C3', 'C4');

-- ---------------------------------------------------------------------
-- 10. Sample Payments
-- ---------------------------------------------------------------------
INSERT INTO payments (payment_id, booking_id, payment_method, payment_status, amount, paid_at) VALUES
(1, 1, 'UPI', 'COMPLETED', 450.00, '2026-11-02 11:31:00');

-- ---------------------------------------------------------------------
-- 11. Sample Cancellations (Demonstration entry for a cancelled booking)
-- ---------------------------------------------------------------------
-- Create a cancelled booking for cancellation testing
INSERT INTO bookings (booking_id, booking_code, user_id, show_id, booking_datetime, total_amount, discount_amount, final_amount, status) VALUES
(2, 'BK-20261105-1002', 3, 2, '2026-11-02 12:00:00', 300.00, 0.00, 300.00, 'CANCELLED');

INSERT INTO cancellations (cancellation_id, booking_id, refund_amount, cancelled_at, reason, status) VALUES
(1, 2, 240.00, '2026-11-02 14:00:00', 'Customer requested cancellation (80% refund policy)', 'PROCESSED');
