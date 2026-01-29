# Plan Testów - Neighborhood Library

## 1. Wprowadzenie i Cele
Celem niniejszego dokumentu jest zdefiniowanie strategii zapewnienia jakości dla projektu "Neighborhood Library". Projekt jest aplikacją typu MVP (Minimum Viable Product) służącą do cyfryzacji biblioteki sąsiedzkiej.

Plan testów został opracowany na podstawie dokumentu wymagań **PRD (Product Requirements Document)** oraz analizy technicznej kodu. Głównym celem jest weryfikacja zgodności implementacji z kryteriami akceptacji zdefiniowanymi w User Stories (US-001 do US-018).

## 2. Zakres Testów

### 2.1. Funkcjonalności objęte testami (In Scope)
*   **Uwierzytelnianie i Profil:** Rejestracja, weryfikacja dwuetapowa, logowanie, reset hasła, edycja profilu.
*   **Zarządzanie Księgozbiorem:** Dodawanie/usuwanie książek, zarządzanie kategoriami.
*   **Wyszukiwanie i Rezerwacja:** Filtrowanie katalogu, proces rezerwacji, limity wypożyczeń.
*   **Obieg Książek:** Wydawanie książek (wypożyczenie), przyjmowanie zwrotów, anulowanie rezerwacji.
*   **Panel Administratora:** Dashboard, zarządzanie użytkownikami (aktywacja, banowanie).
*   **Komunikacja:** System powiadomień wewnętrznych.

### 2.2. Elementy wyłączone z testów (Out of Scope)
*   Integracje z systemami płatności.
*   Obsługa multimediów (zdjęcia okładek).
*   System powiadomień zewnętrznych (E-mail/SMS).
*   Testy wydajnościowe powyżej skali osiedlowej.

## 3. Typy Testów

### 3.1. Testy Jednostkowe (Unit Tests)
*   Weryfikacja logiki biznesowej w warstwie serwisowej (np. limity wypożyczeń, obliczanie dat zwrotu).
*   Testy walidatorów formularzy (Bean Validation).

### 3.2. Testy Integracyjne (Integration Tests)
*   Testy repozytoriów (spójność danych, constrainty bazy PostgreSQL).
*   Testy bezpieczeństwa (Spring Security) weryfikujące dostęp do URLi dla ról USER/ADMIN.

### 3.3. Testy Akceptacyjne (UAT / E2E)
*   Scenariusze oparte bezpośrednio na User Stories, wykonywane przez QA lub zautomatyzowane w przeglądarce.

## 4. Scenariusze Testowe (Mapowanie na User Stories)

Poniższa tabela zawiera szczegółowe przypadki testowe pokrywające wymagania z PRD.

### A. Uwierzytelnianie i Profil Użytkownika

| ID Testu | Powiązane US | Tytuł Scenariusza | Kroki Testowe | Oczekiwany Rezultat (Kryteria Akceptacji) |
|:---|:---|:---|:---|:---|
| **TC-AUTH-01** | US-001 | Rejestracja - sukces | 1. Wypełnij formularz poprawnymi danymi (hasło > 8 znaków).<br>2. Wyślij formularz. | Konto utworzone ze statusem **Nieaktywny**. Wyświetlony komunikat o konieczności wizyty w placówce. |
| **TC-AUTH-02** | US-001 | Rejestracja - walidacja | 1. Wypełnij formularz zduplikowanym loginem lub za krótkim hasłem. | Formularz nie wysłany. Wyświetlone błędy walidacji przy odpowiednich polach. |
| **TC-AUTH-03** | US-002 | Aktywacja konta (Admin) | 1. Zaloguj się jako Admin.<br>2. Przejdź do listy użytkowników.<br>3. Znajdź nieaktywnego usera (na górze listy).<br>4. Kliknij "Aktywuj". | Status zmienia się na **Aktywny**. Użytkownik znika z sekcji "do aktywacji". |
| **TC-AUTH-04** | US-003 | Logowanie - statusy | 1. Próba logowania jako User Nieaktywny.<br>2. Próba logowania jako User Aktywny. | 1. Blokada logowania + komunikat.<br>2. Pomyślne zalogowanie do dashboardu. |
| **TC-AUTH-05** | US-003 | Logowanie - zbanowany | 1. Zaloguj się kontem zbanowanym. | Logowanie skuteczne, ale widoczny wyraźny komunikat o blokadzie. Dostęp ograniczony (brak możliwości rezerwacji). |
| **TC-AUTH-06** | US-004 | Reset hasła (Admin) | 1. Admin wchodzi w edycję użytkownika.<br>2. Klika "Resetuj hasło" i podaje nowe.<br>3. User loguje się nowym hasłem. | Nowe hasło działa. |
| **TC-AUTH-07** | US-005 | Edycja profilu (User) | 1. User wchodzi w "Mój profil".<br>2. Próbuje edytować imię/nazwisko.<br>3. Zmienia hasło. | 1. Pola danych osobowych są zablokowane (read-only).<br>2. Zmiana hasła udana (wymaga podania starego hasła). |

### B. Zarządzanie Księgozbiorem (Admin)

| ID Testu | Powiązane US | Tytuł Scenariusza | Kroki Testowe | Oczekiwany Rezultat (Kryteria Akceptacji) |
|:---|:---|:---|:---|:---|
| **TC-INV-01** | US-006 | Dodanie nowej książki | 1. Admin wypełnia formularz (Tytuł, ISBN, Kategoria).<br>2. Zapisuje. | Książka dodana. System nadał unikalne ID. Domyślny status: **Dostępna**. |
| **TC-INV-02** | US-007 | Usuwanie kategorii zajętej | 1. Admin próbuje usunąć kategorię, do której przypisana jest co najmniej jedna książka. | Operacja zablokowana. Wyświetlony błąd. |
| **TC-INV-03** | US-007 | Edycja kategorii | 1. Admin zmienia nazwę kategorii. | Nazwa zaktualizowana we wszystkich książkach przypisanych do tej kategorii. |
| **TC-INV-04** | US-008 | Usuwanie książki (Hard Delete) | 1. Próba usunięcia książki "Wypożyczonej".<br>2. Zmiana statusu na "Dostępna" i ponowna próba usunięcia. | 1. Blokada usunięcia.<br>2. Książka trwale usunięta z bazy. |

### C. Wyszukiwanie i Rezerwacja (Użytkownik)

| ID Testu | Powiązane US | Tytuł Scenariusza | Kroki Testowe | Oczekiwany Rezultat (Kryteria Akceptacji) |
|:---|:---|:---|:---|:---|
| **TC-SEARCH-01** | US-009 | Filtrowanie katalogu | 1. Wpisz frazę w wyszukiwarkę.<br>2. Wybierz kategorię.<br>3. Szukaj. | Wyniki zawierają tylko pozycje pasujące do OBU kryteriów. Działa stronicowanie. |
| **TC-RES-01** | US-010 | Rezerwacja - sukces | 1. Znajdź książkę ze statusem **Dostępna**.<br>2. Kliknij "Rezerwuj". | Status zmienia się na **Zarezerwowana**. Licznik rezerwacji usera +1. |
| **TC-RES-02** | US-010 | Rezerwacja - limit | 1. Użytkownik ma już 3 aktywne wypożyczenia/rezerwacje.<br>2. Próbuje zarezerwować 4. pozycję. | System blokuje rezerwację. Wyświetla komunikat o limicie (Max 3). |
| **TC-RES-03** | US-011 | Anulowanie rezerwacji | 1. User wchodzi w "Moje książki".<br>2. Anuluje aktywną rezerwację. | Status książki wraca na **Dostępna**. Limit użytkownika zmniejsza się o 1. |

### D. Obieg Książek i Administracja

| ID Testu | Powiązane US | Tytuł Scenariusza | Kroki Testowe | Oczekiwany Rezultat (Kryteria Akceptacji) |
|:---|:---|:---|:---|:---|
| **TC-CIRC-01** | US-012 | Wydanie książki | 1. Admin wchodzi w "Rezerwacje".<br>2. Klika "Wydaj" przy użytkowniku.<br>3. Sprawdź datę zwrotu. | Status -> **Wypożyczona**. Termin zwrotu = Data dzisiejsza + 30 dni. Powiadomienie wysłane do usera. |
| **TC-CIRC-02** | US-013 | Zwrot książki | 1. Admin klika "Przyjmij zwrot". | Status -> **Dostępna**. Wypożyczenie znika z listy aktywnych u użytkownika. |
| **TC-CIRC-03** | US-014 | Anulowanie rezerwacji przez Admina | 1. Admin anuluje rezerwację użytkownika, który nie odebrał książki. | Status -> **Dostępna**. Wymagane potwierdzenie w modalu przed akcją. |
| **TC-ADMIN-01** | US-015 | Dashboard Admina | 1. Zaloguj się jako Admin. | Widoczne liczniki: Nowi użytkownicy, Rezerwacje, Przetrzymane książki. |
| **TC-ADMIN-02** | US-016 | Banowanie użytkownika | 1. Admin blokuje użytkownika.<br>2. Użytkownik próbuje zarezerwować książkę. | Rezerwacja niemożliwa. User otrzymuje powiadomienie o blokadzie. |
| **TC-MSG-01** | US-017 | Powiadomienia | 1. Wygeneruj sytuację przeterminowania zwrotu (np. zmianą daty w bazie). | Licznik wiadomości przy ikonie usera wzrasta. W skrzynce widoczna wiadomość systemowa. |
| **TC-INFO-01** | US-018 | Info o bibliotece | 1. Kliknij "O bibliotece" (jako niezalogowany i zalogowany). | Otwiera się modal z adresem i regulaminem. |

## 5. Środowisko Testowe

*   **Baza danych:** PostgreSQL (zgodnie ze stosem technologicznym). Należy użyć oddzielnej bazy testowej lub schematu, aby nie nadpisywać danych developerskich.
*   **Przeglądarki:** Chrome (najnowsza), Firefox (najnowsza) - testy responsywności (RWD) w narzędziach developerskich przeglądarki.
*   **Dane testowe:** Zestaw danych początkowych (`data.sql`) zawierający:
    *   1 konto Admina.
    *   2-3 konta Userów (Aktywny, Nieaktywny, Zbanowany).
    *   Kategorie i książki w różnych statusach (Dostępna, Wypożyczona).

## 6. Narzędzia do Testowania

*   **Automatyzacja backend:** JUnit 5, Mockito, Spring Boot Test.
*   **Automatyzacja frontend/E2E:** Selenium WebDriver lub Playwright (dla kluczowych ścieżek jak Rezerwacja).
*   **Manualne:** Postman (do testowania API jeśli endpointy są wystawione), DevTools przeglądarki.

## 7. Harmonogram Testów

1.  **Faza 1:** Testy Jednostkowe i Integracyjne serwisów (Auth, LibraryInfo) - Wykonywane na bieżąco przez Developerów.
2.  **Faza 2:** Testy Funkcjonalne UI (Scenariusze TC-AUTH, TC-INV) - Po zaimplementowaniu widoków Admina.
3.  **Faza 3:** Testy Procesowe E2E (Scenariusze TC-RES, TC-CIRC) - Po pełnym spięciu obiegu książki.
4.  **Faza 4:** Testy Regresji i Bezpieczeństwa - Przed finalnym wydaniem.

## 8. Kryteria Akceptacji Testów

Wdrożenie uznaje się za gotowe, gdy:
*   Wszystkie scenariusze o priorytecie krytycznym (Rejestracja, Logowanie, Wypożyczenie, Zwrot) kończą się wynikiem POZYTYWNYM.
*   Wskaźnik pokrycia kodu testami jednostkowymi wynosi min. 70%.
*   Brak błędów blokujących (Severity: Critical/Blocker) w Jirze/GitHub Issues.

## 9. Role i Odpowiedzialności

*   **QA Engineer:** Wykonanie testów manualnych, raportowanie defektów, weryfikacja zgodności z PRD.
*   **Developer:** Naprawa zgłoszonych błędów, utrzymanie testów jednostkowych.
*   **Product Owner:** Ostateczna akceptacja funkcjonalności (UAT) na podstawie scenariuszy.

## 10. Procedury Raportowania Błędów

Zgłoszenie błędu musi zawierać:
*   ID User Story, którego dotyczy błąd.
*   Kroki do reprodukcji.
*   Zrzut ekranu lub logi z aplikacji.
*   Oczekiwane zachowanie vs Rzeczywiste zachowanie.