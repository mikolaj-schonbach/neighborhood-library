package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.AuthService;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;
    @MockitoBean private AuthService authService;

    // --- Mocki dla GlobalControllerAdvice ---
    @MockitoBean private LibraryInfoService libraryInfoService;
    @MockitoBean private MessageService messageService;
    // ----------------------------------------

    private User mockUser;

    @BeforeEach
    void setUp() {
        // Przygotowujemy "zalogowanego" użytkownika
        mockUser = new User();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        mockUser.setLogin("janek");
        mockUser.setFirstName("Jan");
        mockUser.setLastName("Kowalski");

        // Konfigurujemy serwis, żeby zwracał tego użytkownika
        when(currentUserService.requireCurrentUser()).thenReturn(mockUser);
    }

    // --- GET /my/profile ---

    @Test
    @WithMockUser(username = "janek", roles = "USER") // Symulacja zalogowania
    void showProfile_ShouldReturnProfileView_WhenAuthorized() throws Exception {
        mockMvc.perform(get("/my/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("my/profile"))
                .andExpect(model().attribute("user", mockUser))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @Test
    void showProfile_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        // Brak @WithMockUser -> użytkownik niezalogowany
        mockMvc.perform(get("/my/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    // --- POST /my/profile/password ---

    @Test
    @WithMockUser(username = "janek")
    void changePassword_ShouldSucceed_WhenDataIsValid() throws Exception {
        // when
        mockMvc.perform(post("/my/profile/password")
                        .with(csrf())
                        .param("oldPassword", "stare123")
                        .param("newPassword", "nowe1234")
                        .param("confirmNewPassword", "nowe1234")) // Zgodne hasła
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/profile"))
                .andExpect(flash().attributeExists("success"));

        // then
        verify(authService).changePassword(1L, "stare123", "nowe1234");
    }

    @Test
    @WithMockUser(username = "janek")
    void changePassword_ShouldFail_WhenPasswordsDoNotMatch() throws Exception {
        // when
        mockMvc.perform(post("/my/profile/password")
                        .with(csrf())
                        .param("oldPassword", "stare123")
                        .param("newPassword", "nowe1234")
                        .param("confirmNewPassword", "inneHaslo")) // Niezgodne
                .andExpect(status().isOk()) // Zostajemy na stronie (formularz z błędami)
                .andExpect(view().name("my/profile"))
                .andExpect(model().attributeHasFieldErrors("passwordForm", "confirmNewPassword"));

        // then
        verify(authService, never()).changePassword(anyLong(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "janek")
    void changePassword_ShouldFail_WhenOldPasswordIsWrong() throws Exception {
        // given
        // Symulujemy, że serwis rzuca wyjątek (złe stare hasło)
        doThrow(new IllegalArgumentException("Stare hasło jest nieprawidłowe."))
                .when(authService).changePassword(anyLong(), anyString(), anyString());

        // when
        mockMvc.perform(post("/my/profile/password")
                        .with(csrf())
                        .param("oldPassword", "zleStare")
                        .param("newPassword", "nowe1234")
                        .param("confirmNewPassword", "nowe1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("my/profile"))
                .andExpect(model().attributeHasFieldErrors("passwordForm", "oldPassword"));
    }
}
