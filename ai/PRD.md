# Dokument wymagań produktu (PRD) - Neighborhood library



## 1. Przegląd produktu



Aplikacja Neighborhood Library to system typu MVP (Minimum Viable Product) mający na celu cyfryzację procesów w bibliotece sąsiedzkiej. System ma za zadanie zastąpić manualne przeszukiwanie fizycznych półek cyfrowym katalogiem dostępnym online, umożliwiając użytkownikom sprawdzanie dostępności i rezerwację tytułów. Aplikacja usprawnia również pracę bibliotekarza (Administratora) poprzez centralizację bazy danych czytelników, księgozbioru oraz zarządzanie statusem wypożyczeń.

Głównym celem tego wydania jest dostarczenie kluczowych funkcjonalności niezbędnych do obsługi obiegu książek bez zbędnych komplikacji (brak płatności, brak automatyzacji mailowej), skupiając się na interakcji fizycznej wspieranej przez system informatyczny.



## 2. Problem użytkownika



Obecnie korzystanie z biblioteki sąsiedzkiej wiąże się z koniecznością fizycznej obecności w celu sprawdzenia dostępności tytułów. Jest to proces czasochłonny i często kończy się frustracją, gdy poszukiwana pozycja jest niedostępna.

Kluczowe problemy:

* Brak możliwości zdalnego sprawdzenia, czy dana książka jest dostępna.
* Trudność w kontroli terminów zwrotu przez czytelników.
* Czasochłonne zarządzanie papierową kartoteką przez bibliotekarza.
* Brak szybkiej informacji o nowościach w bibliotece.



## 3. Wymagania funkcjonalne



### 3.1 Uwierzytelnianie i Zarządzanie Użytkownikami

* Rejestracja użytkowników z danymi: Imię, Nazwisko, Login, Hasło, Telefon, Adres.
* Dwuetapowy proces dostępu: Rejestracja online -> Fizyczna weryfikacja tożsamości w bibliotece -> Aktywacja konta przez Administratora w systemie.
* Logowanie tylko dla aktywnych użytkowników.
* Możliwość blokowania kont użytkowników (ban) przez Administratora (np. za przetrzymywanie książek).
* Resetowanie hasła realizowane manualnie przez Administratora (nadanie hasła tymczasowego).
* Użytkownik może edytować tylko swoje hasło, ale nie dane tożsamościowe i kontaktowe (imię, nazwisko, login, telefon, adres).


### 3.2 Zarządzanie Księgozbiorem (Administrator)

* Dodawanie książek/czasopism z polami: Tytuł, Autorzy, Rodzaj, Kategoria, ISBN, Rok wydania.
* Każdy fizyczny egzemplarz jest osobnym rekordem w bazie z unikalnym, automatycznie generowanym numerem inwentarzowym (ID).
* Zarządzanie słownikiem Kategorii (dodawanie, edycja, usuwanie - z blokadą usunięcia kategorii niepustej).
* Usuwanie książek (tylko jeśli nie są wypożyczone/zarezerwowane) oraz ich edycja.


### 3.3 Przeglądanie i Wyszukiwanie (Użytkownik)

* Wyszukiwarka tekstowa (Tytuł, Autor) działająca łącznie z filtrem Kategorii.
* Wyświetlanie listy wyników z paginacją (stronicowaniem).
* Prezentacja statusu dostępności dla każdego egzemplarza: Dostępna, Zarezerwowana, Wypożyczona, Niedostępna.


### 3.4 Proces Rezerwacji i Wypożyczania

* Rezerwacja online przez użytkownika (model kto pierwszy, ten lepszy).
* Limit ilościowy: suma wypożyczeń i rezerwacji nie może przekroczyć 3 sztuk na użytkownika.
* Maksymalny czas wypożyczenia: 30 dni.
* Możliwość samodzielnego anulowania rezerwacji przez użytkownika przed odbiorem.
* Administrator zmienia statusy manualnie: Wydanie książki (zmiana z Zarezerwowana na Wypożyczona), Przyjęcie zwrotu (zmiana z Wypożyczona na Dostępna), Anulowanie rezerwacji.


### 3.5 Komunikacja i Powiadomienia

* Wewnętrzny system wiadomości (ikona/badge w aplikacji).
* Automatyczne generowanie komunikatów systemowych przy zdarzeniach: zbliżający się termin zwrotu, przekroczenie terminu, zablokowanie konta.
* Status wiadomości: przeczytana/nieprzeczytana.


### 3.6 Interfejs i UX

* Responsywność (RWD) dla widoków użytkownika (dostęp mobilny).
* Panel Administratora z dashboardem operacyjnym (liczniki zadań).
* Statyczna informacja "O bibliotece" (adres, godziny, regulamin) dostępna w modalu.


## 4. Granice produktu



Poniższe funkcjonalności są wyraźnie wyłączone z zakresu MVP:

* Integracja z systemami płatności (kary są obsługiwane poza systemem lub przez blokadę konta).
* Przesyłanie i wyświetlanie zdjęć okładek książek.
* System recenzji i oceniania książek.
* Wysyłanie wiadomości między użytkownikami (czat) lub od użytkownika do admina.
* Powiadomienia E-mail/SMS (tylko komunikaty wewnątrz aplikacji).
* Możliwość przedłużania terminu zwrotu (prolongata) przez użytkownika lub admina w systemie.
* Historia wypożyczeń archiwalnych dla użytkownika (widoczne tylko bieżące).


## 5. Historyjki użytkowników



### Uwierzytelnianie i Profil



ID: US-001 Rejestracja użytkownika
Tytuł: Rejestracja nowego konta czytelnika
Opis: Jako nowy użytkownik chcę wypełnić formularz rejestracyjny, aby zgłosić chęć dołączenia do biblioteki.
Kryteria akceptacji:

* Formularz zawiera pola: Imię, Nazwisko, Login (unikalny), Hasło, Powtórz hasło, Telefon, Adres.
* Hasło musi mieć minimum 8 znaków.
* Po wysłaniu formularza system tworzy konto ze statusem Nieaktywny.
* Użytkownik widzi komunikat o konieczności udania się do placówki w celu aktywacji konta.


ID: US-002 Aktywacja konta przez Administratora
Tytuł: Aktywacja zweryfikowanego użytkownika
Opis: Jako Administrator chcę aktywować konto użytkownika po weryfikacji jego tożsamości, aby umożliwić mu logowanie.
Kryteria akceptacji:

* Administrator widzi listę użytkowników posortowaną tak, że nieaktywni są na górze.
* Administrator ma przycisk Aktywuj przy użytkowniku.
* Po kliknięciu status zmienia się na Aktywny.


ID: US-003 Logowanie do systemu
Tytuł: Bezpieczne logowanie użytkownika
Opis: Jako użytkownik chcę zalogować się na swoje konto, aby korzystać z funkcji biblioteki.
Kryteria akceptacji:

* Logowanie wymaga podania Loginu i Hasła.
* System blokuje logowanie dla kont o statusie Nieaktywny (wyświetla komunikat o konieczności aktywacji).
* System wpuszcza użytkownika o statusie Aktywny.
* Użytkownik zablokowany (zbanowany) może się zalogować, ale widzi wyraźny komunikat o blokadzie i ma ograniczony dostęp (tylko podgląd zwrotów).


ID: US-004 Reset hasła
Tytuł: Administracyjny reset hasła
Opis: Jako Administrator chcę zresetować hasło użytkownika, który je zapomniał, aby przywrócić mu dostęp.
Kryteria akceptacji:

* Administrator w edycji użytkownika ma przycisk Resetuj hasło.
* Administrator wpisuje nowe hasło ręcznie
* Administrator przekazuje hasło ustnie użytkownikowi.


ID: US-005 Edycja profilu i zmiana hasła
Tytuł: Zarządzanie własnymi danymi
Opis: Jako zalogowany użytkownik chcę zmienić swoje hasło.
Kryteria akceptacji:

* Pola Imię, Nazwisko, Login, Telefon i Adres są zablokowane do edycji.
* Użytkownik może zmienić hasło podając stare i nowe (min. 8 znaków).



### Zarządzanie Księgozbiorem (Admin)



ID: US-006 Dodawanie nowej książki
Tytuł: Rejestracja egzemplarza w systemie
Opis: Jako Administrator chcę dodać nową książkę do bazy, aby była widoczna dla czytelników.
Kryteria akceptacji:

* Formularz zawiera: Tytuł, Autorzy, Rodzaj (Książka/Czasopismo), Kategoria (lista rozwijana), ISBN, Rok wydania.
* System generuje unikalne ID (numer inwentarzowy) dla dodanego rekordu.
* Domyślny status nowej książki to Dostępna.


ID: US-007 Zarządzanie kategoriami
Tytuł: Edycja słownika kategorii
Opis: Jako Administrator chcę dodawać i usuwać kategorie książek, aby utrzymać porządek w zbiorach.
Kryteria akceptacji:

* Administrator może dodać nową nazwę kategorii.
* Administrator może edytować nazwę istniejącej kategorii.
* Próba usunięcia kategorii, do której przypisane są książki, kończy się błędem i komunikatem blokującym.


ID: US-008 Usuwanie książki
Tytuł: Usuwanie zniszczonych lub zagubionych egzemplarzy
Opis: Jako Administrator chcę trwale usunąć książkę z systemu.
Kryteria akceptacji:

* Akcja usuwania wymaga potwierdzenia w oknie modalnym.
* System blokuje usunięcie książki, która ma status Wypożyczona lub Zarezerwowana.
* Jeśli książka została zagubiona przez użytkownika to Administrator najpierw zmienia jej status na Dostępna (czyli w systemie dokonuje przyjęcia zwrotu) a później ją usuwa.
* Jeśli książka ma status Zarezerwowana a z jakiegoś powodu zaginęła to Administrator najpierw zmienia status na Dostępna (anuluje rezerwację) a później ją usuwa.
* Usunięcie jest trwałe (hard delete).



### Wyszukiwanie i Rezerwacja (User)



ID: US-009 Przeglądanie i wyszukiwanie
Tytuł: Wyszukiwanie materiałów bibliotecznych
Opis: Jako użytkownik chcę znaleźć interesującą mnie książkę za pomocą wyszukiwarki.
Kryteria akceptacji:

* Wyszukiwarka pozwala wpisać frazę (szuka w Tytule i Autorze).
* Użytkownik może wybrać Kategorię z listy (tylko kategorie niepuste).
* Wyniki można filtrować po obu kryteriach jednocześnie lub po jednym kryterium (jeśli drugie jest niewypełnione)
* Lista wyników jest stronicowana (paginacja).


ID: US-010 Rezerwacja książki
Tytuł: Rezerwacja dostępnego egzemplarza
Opis: Jako użytkownik chcę zarezerwować dostępną książkę, aby odebrać ją w bibliotece.
Kryteria akceptacji:

* Przycisk Rezerwuj jest aktywny tylko dla statusu Dostępna.
* System sprawdza limit użytkownika (Max 3 sztuki: wypożyczone + zarezerwowane).
* Jeśli limit jest przekroczony, system blokuje rezerwację i wyświetla komunikat.
* Po sukcesie status książki zmienia się na Zarezerwowana.


ID: US-011 Anulowanie rezerwacji
Tytuł: Rezygnacja z rezerwacji
Opis: Jako użytkownik chcę anulować moją rezerwację, jeśli zmieniłem zdanie.
Kryteria akceptacji:

* Użytkownik widzi listę swoich rezerwacji w panelu Moje książki.
* Przy aktywnej rezerwacji dostępny jest przycisk Anuluj.
* Po kliknięciu i potwierdzeniu status książki wraca na Dostępna, a limit użytkownika się zwalnia.



### Obieg Książek (Admin)



ID: US-012 Wydanie książki (Wypożyczenie)
Tytuł: Realizacja wypożyczenia w bibliotece
Opis: Jako Administrator chcę odnotować w systemie wydanie zarezerwowanej książki użytkownikowi.
Kryteria akceptacji:

* Administrator wchodzi w zakładkę Rezerwacje.
* Klika przycisk Wydaj przy odpowiednim tytule i użytkowniku.
* Status książki zmienia się na Wypożyczona.
* System ustawia termin zwrotu na data bieżąca + 30 dni.
* Użytkownik otrzymuje wiadomość systemową o wypożyczeniu.


ID: US-013 Zwrot książki
Tytuł: Przyjęcie zwrotu od czytelnika
Opis: Jako Administrator chcę odnotować zwrot książki, aby znów była dostępna dla innych.
Kryteria akceptacji:

* Administrator przegląda wypożyczenia na liście wypożyczeń.
* Klika przycisk "Przyjmij zwrot".
* Status książki zmienia się na "Dostępna".
* Wypożyczenie znika z listy aktywnych wypożyczeń użytkownika.


ID: US-014 Anulowanie rezerwacji przez Admina
Tytuł: Zwolnienie nieodebranej rezerwacji
Opis: Jako Administrator chcę anulować rezerwację, jeśli użytkownik nie zgłosił się po odbiór.
Kryteria akceptacji:

* Administrator w zakładce Rezerwacje klika Anuluj rezerwację.
* Wymagane potwierdzenie w modalu.
* Status książki zmienia się na Dostępna.



### Zarządzanie Użytkownikami (Admin) i Powiadomienia



ID: US-015 Dashboard Administratora
Tytuł: Podgląd zadań bieżących
Opis: Jako Administrator po zalogowaniu chcę widzieć podsumowanie zadań wymagających uwagi.
Kryteria akceptacji:

* Widok zawiera licznik: Nowi użytkownicy (do aktywacji).
* Widok zawiera licznik: Oczekujące rezerwacje.
* Widok zawiera listę: Książki przetrzymane (po terminie).


ID: US-016 Blokowanie użytkownika (Ban)
Tytuł: Blokada konta za naruszenia
Opis: Jako Administrator chcę zablokować użytkownika, który nie oddał książek w terminie.
Kryteria akceptacji:

* Administrator ma przycisk Zablokuj na profilu użytkownika.
* Zablokowany użytkownik traci możliwość rezerwacji książek.
* Zablokowany użytkownik otrzymuje powiadomienie systemowe o blokadzie.


ID: US-017 System powiadomień i wiadomości
Tytuł: Odbieranie komunikatów systemowych
Opis: Jako użytkownik chcę być informowany o ważnych zdarzeniach dotyczących mojego konta.
Kryteria akceptacji:

* Aplikacja posiada zakładkę Wiadomości.
* Ikona wiadomości posiada licznik nieprzeczytanych powiadomień.
* System automatycznie wysyła wiadomość, gdy minie termin zwrotu.
* Użytkownik może usunąć przeczytaną wiadomość.


ID: US-018 Informacje o bibliotece
Tytuł: Dostęp do danych kontaktowych
Opis: Jako użytkownik (również niezalogowany) chcę sprawdzić godziny otwarcia biblioteki.
Kryteria akceptacji:

* Dostępny przycisk "O bibliotece" (w stopce lub menu).
* Kliknięcie otwiera okno (modal) z adresem, godzinami otwarcia i skróconym regulaminem (limity 3 szt / 30 dni).


## 6. Metryki sukcesu



Aby ocenić skuteczność wdrożenia wersji MVP, monitorowane będą następujące wskaźniki (mierzone w cyklach kwartalnych na podstawie logów systemowych i bazy danych):


### Wskaźnik Retencji (Retention Rate):

* Cel: 80% zarejestrowanych i aktywnych użytkowników loguje się do aplikacji przynajmniej raz na kwartał.
* Sposób pomiaru: Analiza tabeli logów logowania, zliczanie unikalnych ID użytkowników z logowaniem w zakresie dat kwartału w stosunku do ogólnej liczby aktywnych kont.


### Wskaźnik Adopcji (Adoption Rate):

* Cel: 70% aktywnych użytkowników dokonuje wypożyczenia (status Wypożyczona) przynajmniej 1 książki na kwartał.
* Sposób pomiaru: Zapytanie SQL do tabeli historii operacji, zliczające unikalnych użytkowników, którzy mieli przypisaną akcję Wydania książki w danym kwartale.





