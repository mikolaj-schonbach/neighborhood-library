
# Plan implementacji: Neighborhood Library (Java / Spring / Spring Boot / Thymeleaf / Bootstrap / HTMX)

Poniżej masz plan kroków rozwoju **Neighborhood Library (backend + Thymeleaf/Bootstrap/HTMX)**, spójny z PRD, tech-stack i Twoim schematem PostgreSQL (**DB_INIT/DB_FUNCTIONS**). Układ jest iteracyjny (MVP), a jednocześnie zabezpiecza Cię przed walką później z bezpieczeństwem, transakcjami i spójnością danych.

---

## 0) Ustal „kręgosłup” projektu (struktura + konwencje)

### Docelowa struktura pakietów (propozycja)

- `com.example.neighborhood_library`
  - `config` (Security, MVC, scheduling)
  - `domain` (encje + enumy)
  - `repo` (Spring Data JPA)
  - `service` (logika biznesowa, transakcje)
  - `web`
    - `controller` (MVC)
    - `dto` / `form` (Form-backing beans)
    - `viewmodel` (opcjonalnie)
  - `support` (wyjątki, utilsy, mappery)

### Ważna decyzja na start (statusy)

Statusy w DB są `TEXT` → w Javie rób `enum` + `@Enumerated(EnumType.STRING)` z identycznymi wartościami jak w SQL:

- `UserStatus`: `INACTIVE` / `ACTIVE` / `BANNED`
- `AccountRole`: `USER` / `ADMIN`
- `CopyStatus`: `AVAILABLE` / `RESERVED` / `LOANED` / `UNAVAILABLE`
- `ReservationStatus`: `ACTIVE` / `CANCELLED_BY_USER` / `CANCELLED_BY_ADMIN` / `EXPIRED` / `FULFILLED`
- `MessageType`: min. `DUE_SOON` / `OVERDUE` / `ACCOUNT_BANNED` / `LOAN_CREATED` (zgodnie z PRD)

---

## 1) Baza danych: migracje i uruchomienie lokalne

### 1.1 Załóż bazę `neighborhood_library` w PostgreSQL

- Wykonaj skrypt tworzący strukturę bazy danych: **DB_INIT.SQL**
- Wykonaj skrypt dodający funkcje do bazy danych: **DB_FUNCTIONS.SQL**

### 1.2 Konfiguracja `application.yml`

- datasource: Postgres
- ustaw `spring.jpa.hibernate.ddl-auto=validate` (żeby Hibernate nie próbował tworzyć schematu)

**Efekt tej fazy:** aplikacja startuje i waliduje schemat, triggery działają.

---

## 2) Model domeny (JPA) zgodny z Twoim SQL

Zrób encje dokładnie pod tabele:

- `User`
- `Category`
- `Publication`
- `Author`
- `PublicationAuthor` (encja łącząca N:M albo `@ManyToMany` z tabelą join — ja wolę jawnie, bo łatwiej kontrolować)
- `Copy`
- `Reservation`
- `Loan`
- `Message`
- `LoginLog`
- `OperationHistory`
- `LibraryInfo`

### Uwaga krytyczna: triggery sterują `copies.status`

W encji `Copy` traktuj status jako **DB-managed** (w kodzie nie „ustawiaj” go ręcznie poza wyjątkami typu `UNAVAILABLE`). Najbezpieczniej:

- przy tworzeniu kopii ustaw `AVAILABLE`,
- potem statusy niech robi DB przez rezerwacje/wypożyczenia.

---

## 3) Repozytoria + „klocki” serwisów (bez kontrolerów jeszcze)

### 3.1 Repo (Spring Data JPA) — minimum

- `UserRepository` (`findByLogin`, `listInactiveFirst`, `countByStatus`, itp.)
- `CategoryRepository` (list, `existsPublications`, itp.)
- `PublicationRepository` (search + paging)
- `CopyRepository` (wyszukiwanie kopii z publikacją)
- `ReservationRepository` (dla usera, dla admina, aktywne)
- `LoanRepository` (aktywne, po userze, po `inventory_code`)
- `MessageRepository` (unread count, list, hide)
- `LoginLogRepository`, `OperationHistoryRepository`, `LibraryInfoRepository`

### 3.2 Serwisy (transakcje)

- `AuthService` – rejestracja, zmiana hasła (user), reset hasła (admin)
- `UserAdminService` – aktywacja, ban/unban
- `CatalogService` – wyszukiwanie + paginacja + filtry
- `ReservationService` – tworzenie rezerwacji, anulowanie (user/admin)
- `LoanService` – wydanie (utworzenie loan), zwrot (`returned_at`)
- `MessageService` – listowanie, mark-as-read, hide, tworzenie systemowych
- `AuditService` – wpisy do `operations_history`
- `LoginLogService` – wpisy do `login_logs`

**Zasada:** operacje typu „zarezerwuj / wydaj / zwróć” zawsze w `@Transactional`.

---

## 4) Najtrudniejszy fragment MVP: rezerwacja „kto pierwszy ten lepszy”

W DB-plan masz zalecenie `SELECT … FOR UPDATE SKIP LOCKED`. Zrób to w serwisie jako **native query**.

### Krok implementacyjny (ReservationService)

Dla `publicationId` wybierz jedną kopię:

- `copies.status='AVAILABLE'` i `deleted_at is null`
- `FOR UPDATE SKIP LOCKED`

W tej samej transakcji zrób:

- `INSERT reservations(user_id, copy_id, pickup_until, status='ACTIVE')`

**Trigger:**
- sprawdzi ACTIVE usera,
- limit 3,
- czy kopia jest AVAILABLE,
- potem AFTER trigger przeliczy `copies.status` → `RESERVED`.

### Pickup window

PRD nie podaje liczby dni, ale DB wymaga `pickup_until`.
Daj to jako config, np.:

- `app.reservation.pickup-window-hours=48`

---

## 5) Spring Security: role + statusy (INACTIVE/BANNED)

### 5.1 Logowanie

- Form login (standard Spring Security).
- `UserDetailsService` ładuje po login (CITEXT w DB, ale w app i tak traktujesz case-insensitive logiką repo/DB).

### 5.2 Blokada INACTIVE na etapie auth

Jeśli `status=INACTIVE` → nie dopuszczasz do logowania (zgodnie z PRD US-003).

Technicznie: w `UserDetails` możesz zwrócić `enabled=false`, albo rzucić kontrolowany wyjątek i pokazać komunikat na `/login?inactive`.

### 5.3 BANNED – może wejść, ale ma minimalny dostęp

PRD mówi: „może się zalogować, ale tylko podgląd zwrotów”.

Daj dodatkowy authority np. `STATUS_BANNED`.

**Reguły:**
- `/my-books/**` → dostępne dla USER i BANNED
- akcje rezerwacji/zmiany → tylko gdy nie ma `STATUS_BANNED`
- `/admin/**` → `ROLE_ADMIN` (niezależnie od statusu, ale w praktyce admina nie banujesz)

### 5.4 Logowanie metryk (`login_logs`)

`AuthenticationSuccessHandler` i `AuthenticationFailureHandler` zapisują rekordy do `login_logs` (retencja z PRD).

---

## 6) Warstwa MVC + Thymeleaf (iteracje, żeby szybko „coś żyło”)

### Iteracja A – publiczne i auth

**Kontrolery + widoki:**
- `GET /` – landing + linki
- `GET/POST /register` – rejestracja (US-001)
- `GET/POST /login` – logowanie (US-003)

**Fragment/modal „O bibliotece”:**
- `GET /about/fragment` (HTMX) albo wczytanie danych do layoutu (US-018)

**Thymeleaf:**
- `templates/layout.html` (bootstrap + navbar)
- `templates/auth/login.html`
- `templates/auth/register.html`
- `templates/fragments/about-modal.html`

### Iteracja B – katalog (user)

**Backend:**
- `GET /catalog`
- params: `q`, `categoryId`, `page`
- wynik: lista (z paginacją) + status egzemplarza (kopii)

**Frontend (US-009):**
- widok `catalog/index.html`:
  - input tekst
  - select kategorii (tylko niepuste – query: kategorie z publikacjami/kopiami)
  - tabela/lista wyników
  - paginacja (Bootstrap)

### Iteracja C – rezerwacje + „Moje książki”

**User:**
- `POST /reservations/{publicationId}` – „Rezerwuj”
- `GET /my-books` – aktywne rezerwacje + aktywne wypożyczenia (US-011 + „podgląd zwrotów”)
- `POST /reservations/{reservationId}/cancel` – anulowanie przez usera (US-011)

**UI:**
- przy wynikach „Rezerwuj” aktywne tylko gdy `AVAILABLE` i user nie BANNED
- `my-books.html`:
  - sekcja „Rezerwacje” + przycisk „Anuluj”
  - sekcja „Wypożyczenia” + termin zwrotu

**Audyt (`operations_history`):**
- rezerwacja utworzona: `RESERVATION_CREATED`
- anulowanie: `RESERVATION_CANCELLED`

### Iteracja D – panel admina (minimum operacyjne)

**Admin dashboard (US-015):**
- `GET /admin`
- licznik: użytkownicy `INACTIVE`
- licznik: aktywne rezerwacje
- lista: przetrzymane (`loans` gdzie `due_date < today` i `returned_at is null`)

**Użytkownicy (US-002, US-004, US-016):**
- `GET /admin/users`
- `POST /admin/users/{id}/activate`
- `POST /admin/users/{id}/ban` + `POST /admin/users/{id}/unban`
- `POST /admin/users/{id}/reset-password` (generuj hasło tymczasowe)

**Rezerwacje / obieg (US-012, US-013, US-014):**
- `GET /admin/reservations` – lista aktywnych
- `POST /admin/reservations/{id}/issue` → tworzy `loan`
- `POST /admin/reservations/{id}/cancel` → status `CANCELLED_BY_ADMIN`
- `GET /admin/loans` – aktywne
  - wyszukiwanie po `inventory_code` lub nazwisku
- `POST /admin/loans/{id}/return` → ustawia `returned_at=now()`

**Wiadomości systemowe:**
- po utworzeniu `loan`: `LOAN_CREATED` do usera (US-012)
- po banie: `ACCOUNT_BANNED`

### Iteracja E – księgozbiór (admin)

**Kategorie (US-007):**
- `GET /admin/categories`
- `POST /admin/categories` (add)
- `POST /admin/categories/{id}` (rename)
- `POST /admin/categories/{id}/delete`
- jeśli FK `RESTRICT` → pokaż komunikat

**Dodawanie książki (US-006):**
Baza jest 3NF (authors osobno), ale formularz w PRD ma pole „Autorzy”.

- UI: jedno pole tekstowe: „Autorzy (oddziel średnikiem)”
- Serwis:
  - upsert autorów po (`first_name`, `last_name`)
  - utwórz `Publication`
  - powiąż w `publications_authors`
  - utwórz `Copy`

**Inventory code (wymóg formatu):**
Ponieważ `inventory_code` jest `NOT NULL`, a ID dostajesz po insercie, najpraktyczniej w MVP:

- twórz kopię z tymczasowym `inventory_code = "TMP-" + UUID`
- po `save()` znasz `copy.id` → aktualizujesz na `LIB-{YYYY}-{id:06d}` w tej samej transakcji

**Usuwanie książki (US-008) vs schemat:**
DB ma `deleted_at` + check „deleted → UNAVAILABLE”, a FK-y na reservations/loans są `RESTRICT`.
Najbezpieczniej w MVP:

- „Usuń egzemplarz” = soft delete: `deleted_at=now()` + `status=UNAVAILABLE`
- w katalogu filtrujesz `deleted_at is null`

---

## 7) Wiadomości + badge + automaty (PRD 3.5 i US-017)

### 7.1 Widoki

- `GET /messages` – lista (nieukryte)
- `POST /messages/{id}/read` – ustawia `read_at`
- `POST /messages/{id}/hide` – tylko gdy `read_at != null` (DB check to wymusi)

**Badge:**
- w navbarze wyświetl `countUnread` (query do repo)

### 7.2 Automaty „due soon” i „overdue”

PRD wymaga automatycznych komunikatów:
- zbliżający się termin zwrotu
- przekroczenie terminu

Najprościej:
- `@Scheduled(cron=...)` raz dziennie
- query:
  - `due_soon`: `due_date between today and today + X`
  - `overdue`: `due_date < today` i `returned_at is null`
- twórz `Message` odpowiedniego typu

---

## 8) HTMX: gdzie daje realny zysk w MVP

Mało JS, lepszy UX:

**Admin:**
- aktywacja usera / ban / unban → podmień tylko wiersz tabeli
- „Wydaj” / „Anuluj” rezerwację → usuń wiersz z listy + zaktualizuj licznik

**User:**
- „Rezerwuj” → podmień status wiersza bez pełnego reloadu

**Navbar badge wiadomości:**
- opcjonalny endpoint `GET /fragments/message-badge` odświeżany co np. 30–60s (albo tylko po akcjach)

---

## 9) Testy (żeby nie bać się triggerów)

Minimum wartościowe w MVP:

**Testy integracyjne serwisów transakcyjnych (reservation/loan) na Postgresie:**
- idealnie Testcontainers (bo masz triggery i citext)

**Scenariusze:**
- nie da się zarezerwować, gdy user `INACTIVE/BANNED`
- limit 3 działa (DB trigger)
- współbieżność rezerwacji (2 transakcje) – tylko jedna dostaje egzemplarz
- wydanie książki zmienia statusy (`reservation FULFILLED`, `copy LOANED`)
- zwrot ustawia copy `AVAILABLE`

---

## 10) Checklist „ekrany i endpointy” na koniec MVP

### Public
- `/` + modal „O bibliotece” (`library_info`)

### Auth/User
- `/register` (INACTIVE po rejestracji)
- `/login` (blokada INACTIVE, dopuszczenie BANNED)
- `/catalog` (wyszukiwanie+filtry+paginacja)
- `/my-books` (rezerwacje + wypożyczenia)
- `/messages` (read/hide + badge)
- `/profile/password` (zmiana hasła, reszta read-only)

### Admin
- `/admin` (dashboard)
- `/admin/users` (activate/ban/unban/reset password)
- `/admin/categories`
- `/admin/catalog/add` (publication+copy+authors)
- `/admin/reservations` (issue/cancel)
- `/admin/loans` (return + wyszukiwarka)
