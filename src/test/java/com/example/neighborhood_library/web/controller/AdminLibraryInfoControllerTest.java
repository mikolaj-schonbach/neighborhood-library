package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminLibraryInfoController.class)
@Import({SecurityConfig.class, AdminLibraryInfoControllerTest.LibraryInfoServiceMockConfig.class})
class AdminLibraryInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Zamiast @MockitoBean dla LibraryInfoService wstrzykujemy bean z własnym Answer,
     * żeby GlobalControllerAdvice nie dostawał null (co rozwala Thymeleaf na ${libraryInfo.address}).
     */
    @Autowired
    private LibraryInfoService libraryInfoService;

    // --- Mocki dla GlobalControllerAdvice ---
    @MockitoBean
    private MessageService messageService;
    @MockitoBean private CurrentUserService currentUserService;
    // ----------------------------------------

    @BeforeEach
    void setUp() {
        User adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        adminUser.setLogin("admin");
        adminUser.setAccountRole(AccountRole.ADMIN);

        when(currentUserService.requireCurrentUser()).thenReturn(adminUser);
    }

    // --- SECURITY CHECKS ---

    @Test
    void editForm_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/admin/info"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void editForm_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/info"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void update_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(post("/admin/info")
                        .with(csrf())
                        .param("address", "Nowy adres 1")
                        .param("openingHours", "Pn-Pt 10-18")
                        .param("rules", "Nowe zasady"))
                .andExpect(status().isForbidden());
    }

    // --- ACTIONS ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editForm_ShouldReturnView_AndSetActiveNav_ForAdmin() throws Exception {
        mockMvc.perform(get("/admin/info"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/library-info"))
                .andExpect(model().attribute("activeNav", "admin-info"))
                .andExpect(model().attributeExists("libraryInfo")); // kluczowe: ma być w modelu i nie-null (dla Thymeleaf)
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_ShouldCallServiceAndRedirect_WithSuccessFlash() throws Exception {
        mockMvc.perform(post("/admin/info")
                        .with(csrf())
                        .param("address", "Ul. Testowa 1, 00-000 Miasto")
                        .param("openingHours", "Pn-Pt 10:00-18:00")
                        .param("rules", "Cisza w czytelni"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/info"))
                .andExpect(flash().attribute("successMessage", "Zaktualizowano informacje o bibliotece."));

        verify(libraryInfoService).updateInfo(
                "Ul. Testowa 1, 00-000 Miasto",
                "Pn-Pt 10:00-18:00",
                "Cisza w czytelni"
        );
    }

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

                if (rt == Optional.class) {
                    // bezpiecznie: advice, jeśli korzysta z Optional, nie dostanie null
                    return Optional.empty();
                }

                if (rt.isEnum()) {
                    Object[] constants = rt.getEnumConstants();
                    return (constants != null && constants.length > 0) ? constants[0] : null;
                }

                if (rt.isInterface()) {
                    return Mockito.mock(rt);
                }

                // Spróbuj no-args ctor
                try {
                    Constructor<?> c = rt.getDeclaredConstructor();
                    c.setAccessible(true);
                    return c.newInstance();
                } catch (Exception ignored) {
                    // Spróbuj jakikolwiek ctor z parametrami, które umiemy zapełnić
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
                            // ostatnia deska ratunku (może być null – ale zwykle nie będzie potrzebne)
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
