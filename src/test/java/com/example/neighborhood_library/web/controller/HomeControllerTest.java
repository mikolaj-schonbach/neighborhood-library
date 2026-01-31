package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import com.example.neighborhood_library.support.NotFoundException;
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
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, HomeControllerTest.LibraryInfoServiceMockConfig.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // --- Mocki dla GlobalControllerAdvice (jeśli jest w projekcie) ---
    @MockBean private MessageService messageService;
    @MockBean private CurrentUserService currentUserService;

    @Autowired
    private LibraryInfoService libraryInfoService; // bezpieczny bean (żeby Thymeleaf nie dostawał null)
    // ----------------------------------------------------------------

    @Test
    void home_ShouldBeAccessible_ForAnonymous_AndReturnView() throws Exception {
        // jeśli advice próbowałby wymusić currentUser na anonimizmie, realnie powinno to być obsłużone
        // (rzucamy tak jak w realu CurrentUserService)
        when(currentUserService.requireCurrentUser())
                .thenThrow(new NotFoundException("Brak zalogowanego użytkownika"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("activeNav", "home"));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void home_ShouldReturnView_ForLoggedInUser() throws Exception {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", 100L);
        u.setLogin("user1");

        when(currentUserService.requireCurrentUser()).thenReturn(u);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("activeNav", "home"));
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
