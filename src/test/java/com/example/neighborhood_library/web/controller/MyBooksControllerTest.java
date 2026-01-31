package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.Loan;
import com.example.neighborhood_library.domain.Reservation;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import com.example.neighborhood_library.service.ReservationService;
import com.example.neighborhood_library.repo.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MyBooksController.class)
@Import({SecurityConfig.class, MyBooksControllerTest.LibraryInfoServiceMockConfig.class})
class MyBooksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CurrentUserService currentUserService;
    @MockBean private ReservationService reservationService;
    @MockBean private LoanRepository loanRepository;

    // --- Mocki dla GlobalControllerAdvice ---
    @MockBean private MessageService messageService;

    @Autowired
    private LibraryInfoService libraryInfoService; // bezpieczny bean (żeby Thymeleaf nie dostawał null)
    // --------------------------------------

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        ReflectionTestUtils.setField(user, "id", 100L);
        user.setLogin("user1");

        // GlobalControllerAdvice (header) często woła o liczbę nieprzeczytanych
        when(messageService.countUnread(100L)).thenReturn(0L);
    }

    // --- GET /my/books ---

    @Test
    void myBooks_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/my/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(currentUserService, reservationService, loanRepository, messageService);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void myBooks_ShouldReturnViewAndPopulateModel_ForUser() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);

        List<Reservation> reservations = List.of(); // puste -> Thymeleaf nie wejdzie w r.copy.publication.*
        List<Loan> loans = List.of();               // analogicznie dla wypożyczeń

        when(reservationService.myActiveReservations(100L)).thenReturn(reservations);
        when(loanRepository.findByUserIdAndReturnedAtIsNullOrderByLoanedAtDesc(100L)).thenReturn(loans);

        mockMvc.perform(get("/my/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("my/books"))
                .andExpect(model().attribute("activeNav", "my-books"))
                .andExpect(model().attribute("reservations", reservations))
                .andExpect(model().attribute("loans", loans));

        verify(reservationService).myActiveReservations(100L);
        verify(loanRepository).findByUserIdAndReturnedAtIsNullOrderByLoanedAtDesc(100L);

        // z GlobalControllerAdvice:
        verify(messageService).countUnread(100L);

        verifyNoMoreInteractions(reservationService, loanRepository, messageService);
    }


    // --- POST /my/reservations/{reservationId}/cancel ---

    @Test
    void cancel_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(post("/my/reservations/10/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(currentUserService, reservationService, loanRepository, messageService);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void cancel_ShouldRedirectWithSuccessFlash_WhenOk() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);

        mockMvc.perform(post("/my/reservations/55/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/books"))
                .andExpect(flash().attribute("success", "Rezerwacja anulowana ✅"));

        verify(reservationService).cancelByUser(55L, 100L);

        // z GlobalControllerAdvice:
        verify(messageService).countUnread(100L);

        verifyNoMoreInteractions(reservationService, messageService);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void cancel_ShouldRedirectWithErrorFlash_WhenServiceThrows() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        doThrow(new IllegalStateException("Nie można anulować"))
                .when(reservationService).cancelByUser(55L, 100L);

        mockMvc.perform(post("/my/reservations/55/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/books"))
                .andExpect(flash().attribute("error", "Nie można anulować"));

        verify(reservationService).cancelByUser(55L, 100L);

        // z GlobalControllerAdvice:
        verify(messageService).countUnread(100L);

        verifyNoMoreInteractions(reservationService, messageService);
    }

    /**
     * Bezpieczny mock dla Thymeleaf/layoutów (żeby ${libraryInfo.*} nie powodowało NPE).
     */
    @TestConfiguration
    static class LibraryInfoServiceMockConfig {

        @Bean
        @Primary
        LibraryInfoService libraryInfoService() {
            return Mockito.mock(LibraryInfoService.class, invocation -> {
                Class<?> rt = invocation.getMethod().getReturnType();

                if (rt == Void.TYPE) return null;
                if (rt == boolean.class) return false;
                if (rt == byte.class) return (byte) 0;
                if (rt == short.class) return (short) 0;
                if (rt == int.class) return 0;
                if (rt == long.class) return 0L;
                if (rt == float.class) return 0f;
                if (rt == double.class) return 0d;
                if (rt == char.class) return '\0';

                if (rt == String.class) return "";
                if (rt == Boolean.class) return false;
                if (rt == Integer.class) return 0;
                if (rt == Long.class) return 0L;
                if (rt == LocalDate.class) return LocalDate.now();
                if (rt == LocalDateTime.class) return LocalDateTime.now();
                if (rt == Optional.class) return Optional.empty();

                if (rt.isEnum()) {
                    Object[] constants = rt.getEnumConstants();
                    return (constants != null && constants.length > 0) ? constants[0] : null;
                }

                if (rt.isInterface()) return Mockito.mock(rt);

                try {
                    Constructor<?> c = rt.getDeclaredConstructor();
                    c.setAccessible(true);
                    return c.newInstance();
                } catch (Exception ignored) {
                }

                try {
                    Constructor<?>[] ctors = rt.getDeclaredConstructors();
                    Constructor<?> best = null;
                    for (Constructor<?> c : ctors) {
                        if (best == null || c.getParameterCount() < best.getParameterCount()) best = c;
                    }
                    if (best == null) return null;

                    best.setAccessible(true);
                    Object[] args = new Object[best.getParameterCount()];
                    Class<?>[] pts = best.getParameterTypes();

                    for (int i = 0; i < pts.length; i++) {
                        Class<?> pt = pts[i];
                        if (pt == String.class) args[i] = "test";
                        else if (pt == int.class || pt == Integer.class) args[i] = 0;
                        else if (pt == long.class || pt == Long.class) args[i] = 0L;
                        else if (pt == boolean.class || pt == Boolean.class) args[i] = false;
                        else if (pt == LocalDate.class) args[i] = LocalDate.now();
                        else if (pt == LocalDateTime.class) args[i] = LocalDateTime.now();
                        else if (pt.isEnum()) {
                            Object[] constants = pt.getEnumConstants();
                            args[i] = (constants != null && constants.length > 0) ? constants[0] : null;
                        } else if (pt.isInterface()) {
                            args[i] = Mockito.mock(pt);
                        } else {
                            args[i] = null;
                        }
                    }
                    return best.newInstance(args);
                } catch (Exception e) {
                    return null;
                }
            });
        }
    }
}
