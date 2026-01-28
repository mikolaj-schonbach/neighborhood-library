package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.support.DuplicateLoginException;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String firstName,
                         String lastName,
                         String login,
                         String rawPassword,
                         String phone,
                         String address) {

        // szybka walidacja biznesowa (DB i tak broni UNIQUE)
        if (userRepository.existsByLogin(login)) {
            throw new DuplicateLoginException("Login jest już zajęty.");
        }

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setLogin(login.trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        user.setPhone(phone.trim());
        user.setAddress(address.trim());

        user.setStatus(UserStatus.INACTIVE);
        user.setAccountRole(AccountRole.USER);

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // gdyby równoległa rejestracja przeszła existsByLogin()
            throw new DuplicateLoginException("Login jest już zajęty.");
        }
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Użytkownik nie istnieje"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Stare hasło jest nieprawidłowe.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void resetPasswordByAdmin(Long userId, String newRawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Użytkownik nie istnieje"));

        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }
}
