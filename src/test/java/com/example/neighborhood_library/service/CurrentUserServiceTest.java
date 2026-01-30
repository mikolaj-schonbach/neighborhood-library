package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        // Czyścimy kontekst przed każdym testem, żeby nie było "brudów" z innych testów
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireCurrentUser_ShouldReturnUser_WhenAuthenticated() {
        // given
        String login = "activeUser";
        User user = new User();
        user.setLogin(login);

        // Mockowanie Security Context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(login);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByLogin(login)).thenReturn(Optional.of(user));

        // when
        User result = currentUserService.requireCurrentUser();

        // then
        assertEquals(login, result.getLogin());
    }

    @Test
    void requireCurrentUser_ShouldThrowException_WhenAnonymous() {
        // given
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("anonymousUser"); // Spring Security default for unauth

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(securityContext);

        // when & then
        assertThrows(NotFoundException.class, () -> currentUserService.requireCurrentUser());
    }
}