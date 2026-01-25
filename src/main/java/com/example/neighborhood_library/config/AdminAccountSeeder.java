package com.example.neighborhood_library.config;

import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test") // żeby testy integracyjne nie były zanieczyszczane seedem
public class AdminAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // CITEXT => existsByLogin jest case-insensitive (Admin/admin/ADMIN)
        if (userRepository.existsByLogin("Admin")) {
            return;
        }

        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Admin");
        admin.setLogin("Admin");
        admin.setPasswordHash(passwordEncoder.encode("Admin"));
        admin.setPhone("123-456-789");
        admin.setAddress("Biblioteka ul. Biblioteczna.");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setAccountRole(AccountRole.ADMIN);

        try {
            userRepository.save(admin);
            log.warn("Seeded default admin account: login=Admin password=Admin (CHANGE IMMEDIATELY)");
        } catch (DataIntegrityViolationException e) {
            // gdyby dwa procesy startowały równolegle lub ktoś utworzył admina między checkiem a save
            log.info("Admin account already exists (race condition). Skipping seeding.");
        }
    }
}
