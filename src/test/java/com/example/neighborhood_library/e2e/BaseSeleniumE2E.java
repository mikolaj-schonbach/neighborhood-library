package com.example.neighborhood_library.e2e;


import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseSeleniumE2E {

    @LocalServerPort
    protected int port;

    protected WebDriver webDriver;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;


    @BeforeEach
    void setup() {
        // Przygotowanie danych testowych przed uruchomieniem przeglądarki
        createTestUserIfMissing();

        FirefoxOptions options = new FirefoxOptions();
        options.setAcceptInsecureCerts(true);
        // Opcjonalnie: tryb headless (bez okna przeglądarki)
        // options.addArguments("-headless");

        webDriver = new FirefoxDriver(options);
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    private void createTestUserIfMissing() {
        if (!userRepository.existsByLogin("test")) {
            User testUser = new User();
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            testUser.setLogin("test");
            testUser.setPasswordHash(passwordEncoder.encode("test1234"));
            testUser.setPhone("111-222-333");
            testUser.setAddress("Testowa 1, Testowo");
            testUser.setStatus(UserStatus.ACTIVE);
            testUser.setAccountRole(AccountRole.USER);

            userRepository.save(testUser);
        }
    }

    @AfterEach
    void teardown() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }
}