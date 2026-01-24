<conversation_summary>

<decisions>
1. Model domeny publikacji: jedna tabela `publication` z polem `kind` (BOOK/MAGAZINE) i wspólnymi atrybutami (m.in. tytuł, ISBN, rok, kategoria); osobna tabela `copy` (egzemplarz) powiązana z `publication`.
2. Autorzy: osobna tabela `authors` z kolumnami `first_name`, `last_name` oraz tabela łącząca `publication_authors` (relacja N:M).
3. `copy.status` jest cache’em dla UI, ale spójność ma być wymuszana constraintami/triggerami na podstawie tabel `reservation` i `loan`.
4. `reservation` i `loan` zawsze wskazują konkretny `copy_id`; dodatkowo:
   - unikalność aktywnych rekordów per egzemplarz (np. częściowe UNIQUE dla `reservation` i `loan`),
   - atomowa rezerwacja w transakcji.
5. Limit użytkownika: maks. 3 sztuki liczone jako suma aktywnych rezerwacji + aktywnych wypożyczeń; egzekwowane na poziomie DB (np. trigger), niezależnie od walidacji w aplikacji.
6. Status użytkownika: `INACTIVE` (brak logowania), `ACTIVE` (pełny dostęp), `BANNED` (logowanie dozwolone, ale bez rezerwacji; ograniczone widoki).
7. Walidacje/unikalności: `login` unikalny; `isbn` walidacja w aplikacji + opcjonalnie CHECK; `rok_wydania` (SMALLINT) z CHECK i/lub NULL; `adres` jako TEXT (MVP).
8. Wiadomości: `message` z `read_at` oraz mechanizmem ukrywania (soft-delete) przez `visibility` (VISIBLE/HIDDEN) lub `hidden_at`, z regułą „ukrywanie tylko gdy przeczytana”.
9. Logi i metryki: `login_log(user_id, logged_at, success)` oraz `operation_history(actor_user_id, target_user_id, action, copy_id, happened_at)`.
10. RLS/połączenie DB: jedna rola DB dla aplikacji; tożsamość i rola przekazywane przez `current_setting('app.user_id')` i `current_setting('app.role')` ustawiane per transakcję.
11. Publiczny dostęp: publicznie dostępne wyłącznie „O bibliotece” w tabeli `library_info` (SELECT dozwolony publicznie, bez RLS); katalog i reszta chronione.
12. Numer inwentarzowy: niezmienny format `LIB-YYYY-NNNNNN` (z zerowaniem licznika co roku); generacja w DB przez `inventory_counter(year, next_no)` + trigger; blokada zmiany `inventory_code`.
13. Rezerwacje: użytkownik rezerwuje „publikację”, a system wybiera `copy_id` w transakcji przez `SELECT ... FOR UPDATE SKIP LOCKED`, następnie tworzy `reservation(copy_id, user_id)` i aktualizuje `copy.status`.
14. Usuwanie: hard-delete `copy` tylko gdy brak aktywnej rezerwacji/wypożyczenia i status ≠ RESERVED/LOANED; hard-delete `publication` tylko gdy nie ma żadnych `copy` (FK + `ON DELETE RESTRICT` i/lub trigger).
15. Kategorie: `category.name` jako `CITEXT` + UNIQUE; `publication.category_id` z `ON DELETE RESTRICT`; filtr kategorii budowany wyłącznie z „niepustych” kategorii (np. widok/SQL).
16. Wydania/ISBN: częściowy UNIQUE na `publication.isbn` tylko gdy `isbn IS NOT NULL`; różne ISBN = osobne rekordy `publication`; użytkownik widzi oba tytuły i ich ISBN (bez informacji o twardej/miękkiej oprawie).
17. MAGAZINE w MVP: bez dodatkowych pól numeru wydania/issue – zostaje tytuł/rok/ISBN (jeśli jest).
18. Statusy `copy`: dokładnie 4 w UI: AVAILABLE/RESERVED/LOANED/UNAVAILABLE; przypadek „zagubiona” obsługiwany bez dodatkowego statusu (ręczne doprowadzenie do AVAILABLE + hard-delete).
19. Wyszukiwanie: FTS na `publication.search_vector` (GIN), z prefixami (np. `:*`) i stabilnym sortowaniem; opcjonalnie `pg_trgm` później, jeśli substring stanie się wymagany.
20. Autorzy w wyszukiwaniu: `authors_search` zdenormalizowane w `publication`, aktualizowane triggerami po zmianach w relacji autorów i w tabeli `authors`; `search_vector` generowany/aktualizowany triggerem.
21. Terminy i wiadomości: `loan.due_date` jako `DATE`; „zbliżający się termin” = 5 dni przed; „po terminie” = raz; „ban” = raz.
22. Rezerwacje – daty: `reservation.reserved_at` i `pickup_until` (reserved_at + X dni, konfigurowalne); w MVP brak automatycznego joba wygaszającego rezerwacje – `pickup_until` służy do listy zadań/admin działa ręcznie.
23. `loan` powiązane z `reservation`: `loan` musi referencjonować `reservation_id` (lub walidacja, że egzemplarz był RESERVED dla tego samego użytkownika); `due_date = now + 30 dni`.
24. Statusy: `reservation` = ACTIVE/CANCELLED_BY_USER/CANCELLED_BY_ADMIN/EXPIRED/FULFILLED; `loan` = ACTIVE/RETURNED; dodatkowe znaczniki czasu: `cancelled_at`, `fulfilled_at`, `returned_at`.
25. RLS – role aplikacyjne: ANON, USER_INACTIVE, USER_ACTIVE, USER_BANNED, ADMIN; `USER_BANNED` ma SELECT tylko do swoich zwrotów (loans), bez dostępu do katalogu i bez rezerwacji.
26. Reset hasła: brak wymogu „must change password on next login”; użytkownik może korzystać z hasła wygenerowanego przez admina i zmienić je opcjonalnie później.
27. Indeksy: UNIQUE na `copy.inventory_code`; częściowy UNIQUE na `publication.isbn`; indeksy wspierające dashboard i operacje: `loan(due_date)` z filtrem na aktywne, `reservation(status, pickup_until)` pod oczekujące, `users(last_name)`; GIN na `publication.search_vector`.
</decisions>

<matched_recommendations>
1. Rozdzielenie `publication` (dane bibliograficzne) i `copy` (egzemplarz + status) oraz trzymanie `copy.status` jako cache z wymuszeniem spójności triggerami/constraintami.
2. Normalizacja autorów do `authors` + N:M (`publication_authors`) oraz użycie zdenormalizowanego `authors_search` + FTS (`search_vector` + GIN).
3. Bezpieczna współbieżność rezerwacji: transakcja + `FOR UPDATE SKIP LOCKED` oraz częściowe indeksy UNIQUE dla aktywnych rezerwacji/wypożyczeń per egzemplarz.
4. Egzekwowanie limitu 3 aktywnych (rezerwacje+wypożyczenia) na poziomie DB.
5. Modelowanie statusów użytkownika i polityk RLS: ograniczenia dla `BANNED/INACTIVE`, admin pełny dostęp, użytkownik tylko własne dane.
6. Publiczny dostęp tylko do `library_info` (bez RLS), reszta chroniona (RLS + endpointy).
7. Audyt i metryki: `login_log` i `operation_history` z odpowiednimi polami (actor/target/copy/time).
8. Spójne, restrykcyjne zasady hard-delete (copy/publication/category) realizowane przez FK (`ON DELETE RESTRICT`) i/lub trigger.
9. `due_date` jako `DATE`, powiadomienia idempotentne (zbliża się 5 dni, po terminie raz, ban raz).
10. Strategia indeksowania pod główne ścieżki: wyszukiwarka + dashboard + admin-lookup.
</matched_recommendations>

<database_planning_summary>
**a) Główne wymagania dotyczące schematu bazy danych**
- Obsługa katalogu publikacji (BOOK/MAGAZINE) i egzemplarzy z numerem inwentarzowym `LIB-YYYY-NNNNNN` (niezmiennym, generowanym w DB, licznik per rok).
- Rezerwacje i wypożyczenia oparte o konkretne egzemplarze (`copy_id`) z silną spójnością (unikat aktywnych rekordów, transakcje, brak dubli).
- Proces rezerwacji: użytkownik rezerwuje publikację, DB wybiera dostępny egzemplarz (`FOR UPDATE SKIP LOCKED`) i tworzy rezerwację.
- Limit użytkownika: max 3 aktywne sztuki (rezerwacje + wypożyczenia), wymuszony w DB.
- Wyszukiwanie: FTS (tsvector + GIN) po tytule i autorach; autorzy zdenormalizowani do `authors_search` i utrzymywani triggerami.
- Kategorie: `CITEXT` + UNIQUE, tylko niepuste w filtrze, `ON DELETE RESTRICT`.
- Logi/metyki: login_log + operation_history (actor/target/copy/action/time).
- Wiadomości: read/unread + soft-delete (HIDDEN/VISIBLE) tylko dla przeczytanych.

**b) Kluczowe encje i ich relacje**
- `users` (status: INACTIVE/ACTIVE/BANNED; login UNIQUE; hasło edytowalne przez użytkownika).
- `publication` (kind, title, isbn NULLable, year NULLable/checked, category_id, authors_search, search_vector).
- `copy` (publication_id FK, inventory_code UNIQUE, status: AVAILABLE/RESERVED/LOANED/UNAVAILABLE).
- `authors` (first_name, last_name) + `publication_authors` (N:M).
- `category` (name CITEXT UNIQUE) -> `publication` (FK, `ON DELETE RESTRICT`).
- `reservation` (user_id, copy_id, reserved_at, pickup_until, status ENUM + timestamps).
- `loan` (reservation_id, user_id/target, copy_id, due_date DATE, status ACTIVE/RETURNED, returned_at).
- `message` (user_id, type ENUM, content/payload, created_at, read_at, visibility/hidden_at).
- `inventory_counter(year, next_no)` do generacji `copy.inventory_code`.
- `login_log`, `operation_history`.
- `library_info` (publiczny SELECT).

**c) Ważne kwestie bezpieczeństwa i skalowalności**
- RLS oparte o `current_setting('app.user_id')` i `current_setting('app.role')` (jedna rola DB dla aplikacji).
- Role: ANON, USER_INACTIVE, USER_ACTIVE, USER_BANNED, ADMIN.
  - `library_info`: publiczny SELECT (bez RLS).
  - `USER_ACTIVE`: SELECT tylko własne `reservation/loan/message`; katalog dostępny zgodnie z endpointami (chroniony).
  - `USER_BANNED`: SELECT tylko własne `loan` (zwroty), brak katalogu, brak rezerwacji (RLS + WITH CHECK blokujące INSERT/UPDATE/DELETE).
  - `USER_INACTIVE`: brak logowania (logika aplikacji) + ograniczenia dostępu.
  - `ADMIN`: pełny dostęp.
- Wydajność/indeksy:
  - UNIQUE: `copy.inventory_code`, częściowe UNIQUE: `publication.isbn` gdzie NOT NULL.
  - Dashboard: indeksy pod `reservation(status, pickup_until)` i `loan(due_date)` (z filtrem na ACTIVE).
  - Wyszukiwanie: GIN na `publication.search_vector`; stabilne sortowanie; opcjonalnie `pg_trgm` później.
- Spójność i integralność:
  - Triggery/constrainty utrzymujące `copy.status` na podstawie `reservation/loan`.
  - `loan` związany z `reservation_id` + walidacja spójności (ten sam user/copy).
  - Hard-delete ograniczony przez FK/trigger (copy/publication/category).

**d) Nierozwiązane kwestie / obszary do doprecyzowania**
- Dokładna definicja „klucza publikacji” dla anty-duplikacji `publication` (jak normalizować `normalized_title`, czy uwzględniać `year`, czy tylko `isbn` gdy jest).
- Szczegóły generacji `inventory_code`: długość paddingu (np. 6 cyfr), zachowanie przy równoległych insertach (blokada/licznik), ewentualna potrzeba `inventory_code` jako osobnej kolumny vs generowany tekst z (year, seq).
- Dokładna definicja triggerów/przejść statusów: które operacje zmieniają `copy.status` (np. reservation ACTIVE->FULFILLED, loan ACTIVE->RETURNED) i w jakiej kolejności.
- Idempotencja wiadomości (unikalne klucze) i mechanizm ich generowania (job scheduler po stronie aplikacji) — reguły „raz” są ustalone, ale klucz unikalności do doprecyzowania.
- Polityka dostępu do katalogu dla USER_ACTIVE/ANON (ustalone: katalog chroniony, publiczne tylko `library_info`, ale warto doprecyzować: czy ANON ma mieć całkowity brak dostępu do `publication/copy` na poziomie DB/RLS, czy tylko na poziomie API).
</database_planning_summary>

<unresolved_issues>
1. Specyfikacja „unikalnego klucza publikacji” (normalizacja tytułu i kolumny wchodzące w skład UNIQUE/konfliktu).
2. Parametry i implementacja generacji `inventory_code` (padding, format, transakcyjność, blokady) oraz finalny kształt tabeli/licznika.
3. Pełna mapa przejść statusów i triggery dla `copy.status` vs `reservation.status` vs `loan.status` (szczególnie anulowania/zwroty/fulfillment).
4. Schemat idempotencji wiadomości (unikalne ograniczenia), oraz gdzie i jak uruchamiane jest generowanie (harmonogram w aplikacji).
5. Ostateczne doprecyzowanie polityk RLS dla `publication/copy` per rola (ANON/USER_ACTIVE/USER_INACTIVE) w DB vs w API.
</unresolved_issues>

</conversation_summary>
