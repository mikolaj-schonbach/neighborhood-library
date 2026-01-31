package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.User; // Import
import com.example.neighborhood_library.service.AuthService;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import com.example.neighborhood_library.support.DuplicateLoginException;
import com.example.neighborhood_library.support.NotFoundException; // Import
import org.junit.jupiter.api.BeforeEach; // Import
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private LibraryInfoService libraryInfoService;

    @MockBean
    private MessageService messageService;

    @MockBean
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        // Symulujemy zachowanie dla niezalogowanego użytkownika (domyślne w AuthController)
        // GlobalControllerAdvice prawdopodobnie łapie ten wyjątek i zwraca 0 wiadomości
        doThrow(new NotFoundException("Brak zalogowanego użytkownika"))
                .when(currentUserService).requireCurrentUser();
    }

    // --- GET /register ---

    @Test
    void registerForm_ShouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerForm"));
    }

    // --- POST /register ---

    @Test
    void registerSubmit_ShouldRedirect_WhenDataIsValid() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("login", "jank")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("phone", "123456789")
                        .param("address", "Ulica 1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register?success"));

        verify(authService).register("Jan", "Kowalski", "jank", "secret123", "123456789", "Ulica 1");
    }

    @Test
    void registerSubmit_ShouldShowError_WhenPasswordsDoNotMatch() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("login", "jank")
                        .param("password", "secret123")
                        .param("confirmPassword", "inneHaslo")
                        .param("phone", "123456789")
                        .param("address", "Ulica 1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "confirmPassword"));

        verifyNoInteractions(authService);
    }

    @Test
    void registerSubmit_ShouldShowError_WhenLoginIsDuplicate() throws Exception {
        doThrow(new DuplicateLoginException("Login zajęty"))
                .when(authService).register(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("login", "zajetyLogin")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("phone", "123")
                        .param("address", "Ulica 1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "login"));
    }

    // --- GET /login ---

    @Test
    void loginPage_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }
}