package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = (auth != null ? auth.getName() : null);

        if (login == null || "anonymousUser".equalsIgnoreCase(login)) {
            throw new NotFoundException("Brak zalogowanego użytkownika");
        }

        return userRepository.findByLogin(login)
                .orElseThrow(() -> new NotFoundException("Użytkownik nie istnieje: " + login));
    }
}
