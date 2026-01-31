package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import com.example.neighborhood_library.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogReservationController.class)
@Import(SecurityConfig.class)
class CatalogReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private CurrentUserService currentUserService;

    // --- Mocki dla GlobalControllerAdvice (jeśli jest w projekcie) ---
    @MockitoBean private LibraryInfoService libraryInfoService;
    @MockitoBean private MessageService messageService;
    // ---------------------------------------------------------------

    @BeforeEach
    void setUp() {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", 100L);
        u.setLogin("user1");

        when(currentUserService.requireCurrentUser()).thenReturn(u);
    }

    @Test
    void reserve_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(post("/catalog/10/reserve").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(reservationService);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void reserve_ShouldCreateReservation_AndRedirectWithSuccessFlash_WhenOk() throws Exception {
        mockMvc.perform(post("/catalog/10/reserve").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalog/10"))
                .andExpect(flash().attribute("success", "Rezerwacja utworzona ✅"));

        verify(reservationService).reservePublication(10L, 100L);
        verifyNoMoreInteractions(reservationService);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void reserve_ShouldRedirectWithErrorFlash_WhenServiceThrows() throws Exception {
        doThrow(new IllegalStateException("Brak dostępnych egzemplarzy"))
                .when(reservationService).reservePublication(10L, 100L);

        mockMvc.perform(post("/catalog/10/reserve").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalog/10"))
                .andExpect(flash().attribute("error", "Brak dostępnych egzemplarzy"));

        verify(reservationService).reservePublication(10L, 100L);
        verifyNoMoreInteractions(reservationService);
    }
}
