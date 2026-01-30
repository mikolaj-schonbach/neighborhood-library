package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.support.DuplicateLoginException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldEncodePasswordAndSaveUser() {
        // given
        String rawPassword = "secretPassword";
        String encodedPassword = "encodedHash";
        when(userRepository.existsByLogin("janek")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // when
        authService.register("Jan", "Kowalski", "janek", rawPassword, "123", "Street");

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("janek", saved.getLogin());
        assertEquals(encodedPassword, saved.getPasswordHash());
        assertNotNull(saved.getAccountRole());
    }

    @Test
    void register_ShouldThrowException_WhenLoginExists() {
        // given
        when(userRepository.existsByLogin("janek")).thenReturn(true);

        // when & then
        assertThrows(DuplicateLoginException.class, () ->
                authService.register("Jan", "K", "janek", "pass", "123", "Addr")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_ShouldUpdatePassword_WhenOldPasswordIsCorrect() {
        // given
        Long userId = 1L;
        String oldPass = "old";
        String newPass = "new";

        User user = new User();
        user.setPasswordHash("oldHash");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPass, "oldHash")).thenReturn(true);
        when(passwordEncoder.encode(newPass)).thenReturn("newHash");

        // when
        authService.changePassword(userId, oldPass, newPass);

        // then
        assertEquals("newHash", user.getPasswordHash());
        verify(userRepository).save(user);
    }
}