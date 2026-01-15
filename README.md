# Neighborhood Library

![Java](https://img.shields.io/badge/Java-17-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-green?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?logo=postgresql)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)

## 1. Project Description

**Neighborhood Library** is a Minimum Viable Product (MVP) designed to digitize the operations of a local community library. The primary goal of this application is to move away from physical card catalogs and manual shelf-searching, offering a streamlined online experience for both readers and librarians.

Developed as part of the **10xDevs Course**, this system addresses the frustration of physical availability checks by providing a digital catalog. It allows users to browse the collection and reserve titles online, while enabling librarians (Administrators) to manage users, inventory, and loan cycles efficiently.

### Key Features
* **Digital Catalog:** Search books by title, author, and category with real-time availability status.
* **Reservation System:** Users can reserve up to 3 items online (First Come, First Served model).
* **User Management:** Two-step verification process (Online Registration -> Physical Verification -> Admin Activation).
* **Inventory Control:** Management of physical copies, unique inventory IDs, and categorization.
* **Internal Notifications:** System alerts for return deadlines and account statuses.

---

## 2. Table of Contents
1.  [Project Description](#1-project-description)
2.  [Tech Stack](#3-tech-stack)
3.  [Getting Started Locally](#4-getting-started-locally)
4.  [Available Scripts](#5-available-scripts)
5.  [Project Scope](#6-project-scope)
6.  [Project Status](#7-project-status)
7.  [License](#8-license)

---

## 3. Tech Stack

The application uses a monolithic architecture with server-side rendering, focusing on simplicity and stability.

### Backend
| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 17 | Core business logic. |
| **Framework** | Spring Boot 3.5.7 | Application configuration and HTTP server. |
| **Web** | Spring Web (MVC) | REST endpoints and Controller mapping. |
| **Persistence** | Spring Data JPA / Hibernate | ORM mapping for PostgreSQL. |
| **Security** | Spring Security | Auth, Role management, and endpoint protection. |
| **Validation** | Jakarta Bean Validation | Input validation (@NotNull, @Size, etc.). |

### Frontend
| Component | Technology | Description |
| :--- | :--- | :--- |
| **Templating** | Thymeleaf | Server-side HTML generation. |
| **Styling** | Bootstrap 5 (CDN) | Responsive layout and UI components. |
| **Interactivity** | HTMX | Lightweight AJAX partial page reloads. |

### Database & Tools
* **Database:** PostgreSQL
* **Build Tool:** Maven
* **CI/CD:** GitHub Actions
* **Testing:** JUnit 5, Mockito

---

## 4. Getting Started Locally

Follow these instructions to set up the project on your local machine.

### Prerequisites
* **Java 17** SDK installed.
* **Maven** installed (or use the provided wrapper).
* **PostgreSQL** installed and running.

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/neighborhood-library.git](https://github.com/your-username/neighborhood-library.git)
    cd neighborhood-library
    ```

2.  **Database Setup:**
    Create a new PostgreSQL database (e.g., `neighborhood_library`) to store users, books, and reservations.
    ```sql
    CREATE DATABASE neighborhood_library;
    ```

3.  **Configuration:**
    Navigate to `src/main/resources/application.properties` (or `application.yml`) and configure your database credentials:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/neighborhood_library
    spring.datasource.username=your_postgres_user
    spring.datasource.password=your_postgres_password
    spring.jpa.hibernate.ddl-auto=update
    ```

4.  **Build the project:**
    ```bash
    mvn clean install
    ```

5.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```

The application will be accessible at `http://localhost:8080` (default Spring Boot port).

---

## 5. Available Scripts

In the project directory, you can run the following Maven commands:

### `mvn spring-boot:run`
Runs the application in development mode.

### `mvn clean install`
Builds the application and installs the artifact to your local repository. This also executes the test suite.

### `mvn test`
Launches the test runner (JUnit 5) to execute unit and integration tests.

---

## 6. Project Scope

The following outlines the functional boundaries of the MVP version as defined in the PRD.

### ✅ In Scope (Implemented)
* **Authentication:** Registration, Admin-only activation, Login, Ban/Block functionality.
* **Inventory:** CRUD operations for Books/Periodicals with ISBN and Categories.
* **Search:** Text search with Category filtering and pagination.
* **Circulation Rules:**
    * Max 3 items (Loaned + Reserved) per user.
    * Max 30 days loan period.
    * Manual status updates by Admin (Issue/Return).
* **Notifications:** Internal "badge" system for system messages.

### ❌ Out of Scope (Not Implemented)
* **Payments:** No integration with payment gateways; fines are handled externally.
* **Media:** No cover images for books.
* **Social:** No reviews, ratings, or user-to-user chat.
* **External Comms:** No Email or SMS notifications.
* **History:** No archival history view for users (only current loans visible).
* **Prolongation:** No option to extend loan deadlines.

---

## 7. Project Status

**Current Version:** `0.0.1-SNAPSHOT`

This project is currently in the **MVP Development** phase. Core features regarding user registration and inventory management are prioritized.

---

## 8. License

This project is created for educational purposes as part of the **10xDevs Course**.
License rights are not explicitly defined in the provided configuration; please contact the repository owner before redistributing.