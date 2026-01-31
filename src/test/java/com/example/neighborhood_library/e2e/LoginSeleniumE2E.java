
package com.example.neighborhood_library.e2e;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginSeleniumE2E extends BaseSeleniumE2E {

    @Test
    void shouldLoginSuccessfully() {
        // Przejście na stronę logowania
        webDriver.get("http://localhost:" + port + "/login");

        // Lokalizowanie elementów formularza
        // ZMIANA: wait i zmiana nazwy selektora z "username" na "login" zgodnie z HTML
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        WebElement usernameInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("login")));

        WebElement passwordInput = webDriver.findElement(By.name("password"));
        WebElement submitButton = webDriver.findElement(By.cssSelector("button[type='submit']"));

        // Wprowadzanie danych
        usernameInput.sendKeys("test");
        passwordInput.sendKeys("test1234");

        // Wysłanie formularza
        submitButton.click();

        // Oczekiwanie na element potwierdzający zalogowanie (z data-testid)
        WebElement loggedUserElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='auth-username']")
        ));

        // Asercja - sprawdzenie czy użytkownik jest zalogowany
        assertTrue(loggedUserElement.isDisplayed(), "Element z nazwą użytkownika powinien być widoczny");
        assertEquals("test", loggedUserElement.getText(), "Zalogowany użytkownik powinien mieć nazwę 'test'");
    }
}