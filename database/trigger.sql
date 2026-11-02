-- =====================================================================
-- CineBook – Movie Ticket Booking System
-- Database Trigger Script
-- Trigger: trg_update_seat_status
-- Member 3 Responsibility: Seat Status Synchronization
-- Target DBMS: MySQL 8.0+
-- =====================================================================

USE cinebook_db;

DROP TRIGGER IF EXISTS trg_update_seat_status;

DELIMITER $$

CREATE TRIGGER trg_update_seat_status
AFTER INSERT ON booking_seats
FOR EACH ROW
BEGIN
    UPDATE show_seats
    SET status = 'BOOKED'
    WHERE show_seat_id = NEW.show_seat_id;
END$$

DELIMITER ;
