## Backend

* **Java 17** – język programowania, w którym piszę całą logikę biznesową aplikacji.
* **Spring Boot 3.5.7** – główny framework, który upraszcza konfigurację aplikacji i uruchamianie serwera HTTP.
* **Spring Web (Spring MVC)** – obsługuje endpointy HTTP i mapowanie żądań na kontrolery.
* **Spring Data JPA** – ułatwia pracę z bazą danych przez repozytoria zamiast „gołego” SQL.
* **Hibernate** – implementacja JPA, która faktycznie mapuje encje Java na tabele w Postgresie.
* **Spring Security** – odpowiada za logowanie, role, dostęp do widoków i ochronę endpointów.
* **Jakarta Bean Validation (np. @NotNull, @Size)** – waliduje dane wejściowe w formularzach i DTO/encjach.



## Frontend (warstwa widoków)

* **Thymeleaf** – silnik szablonów generujący HTML po stronie serwera na podstawie modeli z kontrolerów.
* **Bootstrap 5 (CDN)** – framework CSS/JS zapewniający responsywny layout, gotowe komponenty i „ładne” UI.
* **HTMX** – mała biblioteka JS do częściowych przeładowań fragmentów strony (AJAX) za pomocą atrybutów w HTML.



## Baza danych

* **PostgreSQL** – relacyjna baza danych przechowująca użytkowników, książki, rezerwacje, wypożyczenia i wiadomości.



## CI/CD

* **GitHub Actions** – narzędzie CI/CD do automatycznego budowania, testowania i wdrażania aplikacji z repozytorium.



## Build i testy (domknięcie stacku)

* **Maven** – system budowania i zarządzania zależnościami projektu Java.
* **JUnit 5** – framework do pisania testów jednostkowych i integracyjnych.
* **Mockito (opcjonalnie)** – biblioteka do mockowania zależności w testach.
