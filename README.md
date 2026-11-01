# CineBook – Movie Ticket Booking System
## Member 1 Module Documentation

### Overview
This repository contains **Member 1's implementation** of **CineBook**, a terminal-based console application for movie ticket booking built with **Java 17**, **JDBC**, **MySQL 8**, and **Maven**.

Member 1 owns the **database architecture, connection management, user management, movie management, theatre management, screen management, and show scheduling with independent seat tracking**.

---

### Branch Information
- **Assigned Branch:** `feature/movie-management`

---

### Database Architecture (`cinebook_db`)
The schema enforces relationships across all **11 shared tables**:
1. `users` – User accounts with roles (`ADMIN`, `CUSTOMER`, `STAFF`) and SHA-256 hashed passwords.
2. `movies` – Movie catalog with title, language, genre, duration, and release date.
3. `theatres` – Cinema complexes with name, city, and address.
4. `screens` – Individual halls/auditoriums belonging to a theatre.
5. `seats` – Physical seats configured per screen with row/col code and tier (`REGULAR`, `PREMIUM`, `VIP`).
6. `shows` – Show screenings linking movies and screens with date and time.
7. `show_seats` – Live, independent seat availability instances for each scheduled show (`AVAILABLE`, `BOOKED`, `LOCKED`).
8. `bookings` – Booking records with total, discount, and final amounts using `DECIMAL(10,2)`.
9. `booking_seats` – Line items mapping bookings to individual show seats.
10. `payments` – Payment transaction records (`UPI`, `CREDIT_CARD`, `DEBIT_CARD`, `NET_BANKING`, `CASH`).
11. `cancellations` – Cancellation and refund audit tracking records.

#### Database Deliverables
- `database/schema.sql`: Full DDL creation script for MySQL 8 with PKs, FK cascades, unique constraints, and check constraints.
- `database/sample_data.sql`: Seed data containing sample users, movies, theatres, screens, 40-seat grids, scheduled shows, show seats, bookings, and payments.

---

### Component & File Manifest

| Package / Directory | File | Responsibility |
|---|---|---|
| `database/` | `schema.sql` | 11-table DDL schema definitions for MySQL 8 |
| `database/` | `sample_data.sql` | Seed data and sample testing records |
| `com.cinebook.database` | `DBConnection.java` | Centralized JDBC connection factory; loads `db.properties` dynamically; provides detailed diagnostic error reporting |
| `com.cinebook.model` | `User.java` | Entity model for `users` table |
| `com.cinebook.model` | `Movie.java` | Entity model for `movies` table |
| `com.cinebook.model` | `Theatre.java` | Entity model for `theatres` table |
| `com.cinebook.model` | `Screen.java` | Entity model for `screens` table with associated seat collection |
| `com.cinebook.model` | `Show.java` | Entity model for `shows` table with joined movie & theatre metadata |
| `com.cinebook.model` | `Seat.java` | Model representing physical seats on a screen |
| `com.cinebook.model` | `ShowSeat.java` | Model representing independent live seat availability for a show |
| `com.cinebook.dao` | `UserDAO.java` | CRUD operations for users; duplicate email validation; PreparedStatement & try-with-resources |
| `com.cinebook.dao` | `MovieDAO.java` | CRUD and search operations for movies |
| `com.cinebook.dao` | `TheatreDAO.java` | CRUD and city filtering for theatres |
| `com.cinebook.dao` | `ScreenDAO.java` | Screen CRUD and batch physical seat creation (default 5x8 grid) |
| `com.cinebook.dao` | `ShowDAO.java` | Show scheduling, automated show-seats generation, and seat status queries |
| `com.cinebook.service` | `UserService.java` | Customer registration, login verification, SHA-256 password hashing |
| `com.cinebook.service` | `MovieService.java` | Business logic for movies, theatres, screens, and seat layouts |
| `com.cinebook.service` | `ShowService.java` | Show scheduling, live seat map rendering (`[O]` / `[X]`), availability tracking |
| `com.cinebook.util` | `PasswordUtil.java` | SHA-256 password hashing and secure verification |
| `com.cinebook.util` | `ConfigLoader.java` | Application-level property loading helper |
| `src/main/resources/` | `db.properties.example` | Template for local database credentials (tracked in git) |
| `src/main/resources/` | `db.properties` | Local database credentials (ignored by `.gitignore`) |
| `src/test/java/com/cinebook` | `Member1IntegrationTest.java` | Comprehensive integration tests verifying DB connectivity, user registration/login, movie/theatre/screen creation, and show scheduling with independent seat availability |
| `/` | `pom.xml` | Maven project build configuration for Java 17, MySQL Connector/J, and JUnit 5 |
| `/` | `.gitignore` | Prevents checking in compiled classes, build artifacts, and sensitive credentials |

---

### Functional Guarantees
1. **User Registration & Login:** Enforces email uniqueness, SHA-256 password hashing, and clean exception messages on authentication failures.
2. **Movie & Theatre Management:** Adding, editing, and listing movies and cinema theatres.
3. **Screen & Seat Association:** Screens are paired with their physical seat grid (Rows A-B Regular @ Rs. 150, Rows C-D Premium @ Rs. 250, Row E VIP @ Rs. 400).
4. **Show Scheduling & Independent Availability:** Scheduling a show automatically clones screen seats into `show_seats` for that specific show. Updating seat availability on Show A does not affect Show B.
5. **Security & Clean JDBC:** No hardcoded credentials. All SQL queries use `PreparedStatement` within `try-with-resources`.

---

### Team Integration Requirements (for Member 2 and Member 3)
1. **Member 2 (Booking & Payments):**
   - Use `ShowService.getShowSeats(showId)` or `ShowDAO.getShowSeats(showId)` to fetch available seats.
   - Insert records into `bookings`, `booking_seats`, and `payments` using `DECIMAL(10,2)` for monetary values.
   - Set seat status in `show_seats` to `'BOOKED'` when a booking is confirmed.
2. **Member 3 (Console UI, Menu Navigation, Stored Procedures/Triggers):**
   - Provide the single application entry point (`Main.java` or `CineApp.java`).
   - Coordinate with Member 1 before adding triggers or stored procedures in `cinebook_db`.
