-- =====================================================================
-- CineBook – Movie Ticket Booking System
-- Database Stored Procedures Script
-- Procedure: sp_cancel_booking
-- Member 3 Responsibility: Cancellation, Seat Release, and Refund Processing
-- Target DBMS: MySQL 8.0+
-- =====================================================================

USE cinebook_db;

DROP PROCEDURE IF EXISTS sp_cancel_booking;

DELIMITER $$

CREATE PROCEDURE sp_cancel_booking(
    IN p_booking_id INT,
    OUT p_refund_amount DECIMAL(10,2)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_status VARCHAR(20);
    DECLARE v_final_amount DECIMAL(10,2);
    DECLARE v_cancellation_count INT DEFAULT 0;

    -- Exit handler: rollback and propagate exception on error
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- 1 & 2. Verify that the booking exists
    SELECT COUNT(*)
    INTO v_exists
    FROM bookings
    WHERE booking_id = p_booking_id;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Booking not found with specified ID.';
    END IF;

    -- Retrieve current booking status and final amount
    SELECT status, final_amount
    INTO v_status, v_final_amount
    FROM bookings
    WHERE booking_id = p_booking_id;

    -- 3. Reject already-cancelled bookings
    IF v_status = 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Booking is already cancelled.';
    END IF;

    -- Check for duplicate cancellation records
    SELECT COUNT(*)
    INTO v_cancellation_count
    FROM cancellations
    WHERE booking_id = p_booking_id;

    IF v_cancellation_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate cancellation: refund record already exists for this booking.';
    END IF;

    -- 4. Calculate 80% of final_amount (Do not use total_amount before coupon discount)
    SET p_refund_amount = ROUND(v_final_amount * 0.80, 2);

    -- 5. Release all seats associated with the booking back to AVAILABLE
    UPDATE show_seats
    SET status = 'AVAILABLE'
    WHERE show_seat_id IN (
        SELECT show_seat_id
        FROM booking_seats
        WHERE booking_id = p_booking_id
    );

    -- 6. Update booking status to CANCELLED
    UPDATE bookings
    SET status = 'CANCELLED'
    WHERE booking_id = p_booking_id;

    -- 7. Update payment status to REFUNDED
    UPDATE payments
    SET payment_status = 'REFUNDED'
    WHERE booking_id = p_booking_id;

    -- 8. Insert a cancellation record with status COMPLETED
    INSERT INTO cancellations (booking_id, refund_amount, cancelled_at, reason, status)
    VALUES (p_booking_id, p_refund_amount, NOW(), 'Customer requested cancellation (80% refund policy)', 'COMPLETED');

    COMMIT;

    -- 9. Return the refund amount
    SELECT p_refund_amount AS refund_amount;
END$$

DELIMITER ;
