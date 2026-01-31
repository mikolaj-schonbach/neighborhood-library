package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.AdminLoanService;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminLoansController.class)
@Import({SecurityConfig.class, AdminLoansControllerTest.LibraryInfoServiceMockConfig.class})
class AdminLoansControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminLoanService adminLoanService;

    // --- Mocki dla GlobalControllerAdvice ---
    @MockBean private MessageService messageService;
    @MockBean private CurrentUserService currentUserService;

    @Autowired
    private LibraryInfoService libraryInfoService;
    // ---------------------------------------

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
    void list_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/admin/loans"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void list_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/loans"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void acceptReturn_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(post("/admin/loans/1/return").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- ACTIONS ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void list_ShouldReturnViewAndModel_WithDefaultPagination_ForAdmin() throws Exception {
        // given
        Page<?> emptyPage = Page.empty();
        when(adminLoanService.activeLoans(PageRequest.of(0, 20))).thenReturn((Page) emptyPage);

        // when + then
        mockMvc.perform(get("/admin/loans"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/loans"))
                .andExpect(model().attribute("activeNav", "admin"))
                .andExpect(model().attribute("loansPage", emptyPage));

        verify(adminLoanService).activeLoans(PageRequest.of(0, 20));
        verifyNoMoreInteractions(adminLoanService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void list_ShouldPassCustomPaginationParams_ForAdmin() throws Exception {
        // given
        Page<?> emptyPage = Page.empty();
        when(adminLoanService.activeLoans(PageRequest.of(2, 5))).thenReturn((Page) emptyPage);

        // when + then
        mockMvc.perform(get("/admin/loans")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/loans"))
                .andExpect(model().attribute("activeNav", "admin"))
                .andExpect(model().attribute("loansPage", emptyPage));

        verify(adminLoanService).activeLoans(PageRequest.of(2, 5));
        verifyNoMoreInteractions(adminLoanService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void acceptReturn_ShouldRedirectWithSuccessFlash_WhenServiceSucceeds() throws Exception {
        mockMvc.perform(post("/admin/loans/123/return")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/loans"))
                .andExpect(flash().attribute("success", "Zwrot przyjęty ✅"));

        verify(adminLoanService).acceptReturn(123L);
        verifyNoMoreInteractions(adminLoanService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void acceptReturn_ShouldRedirectWithErrorFlash_WhenServiceThrows() throws Exception {
        doThrow(new IllegalStateException("Błąd zwrotu"))
                .when(adminLoanService).acceptReturn(123L);

        mockMvc.perform(post("/admin/loans/123/return")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/loans"))
                .andExpect(flash().attribute("error", "Błąd zwrotu"));

        verify(adminLoanService).acceptReturn(123L);
        verifyNoMoreInteractions(adminLoanService);
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

                if (rt == Optional.class) {
                    return Optional.empty();
                }

                if (rt.isEnum()) {
                    Object[] constants = rt.getEnumConstants();
                    return (constants != null && constants.length > 0) ? constants[0] : null;
                }

                if (rt.isInterface()) {
                    return Mockito.mock(rt);
                }

                // no-args ctor
                try {
                    Constructor<?> c = rt.getDeclaredConstructor();
                    c.setAccessible(true);
                    return c.newInstance();
                } catch (Exception ignored) {
                }

                // ctor o najmniejszej liczbie parametrów
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
