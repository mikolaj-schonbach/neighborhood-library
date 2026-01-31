package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.service.AuthService;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import com.example.neighborhood_library.service.UserAdminService;
import com.example.neighborhood_library.web.dto.EditProfileForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUsersController.class)
@Import({SecurityConfig.class, AdminUsersControllerTest.LibraryInfoServiceMockConfig.class})
class AdminUsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserRepository userRepository;
    @MockBean private UserAdminService userAdminService;
    @MockBean private AuthService authService;

    // --- Mocki dla GlobalControllerAdvice ---
    @MockBean private MessageService messageService;
    @MockBean private CurrentUserService currentUserService;

    @Autowired
    private LibraryInfoService libraryInfoService; // bezpieczny bean (żeby Thymeleaf nie dostawał null)
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
    void users_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void users_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void activate_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(post("/admin/users/10/activate").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- GET /admin/users ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void users_ShouldReturnViewAndPopulateLists_ForAdmin() throws Exception {
        User u1 = new User(); u1.setLogin("inactive1"); u1.setStatus(UserStatus.INACTIVE);
        User u2 = new User(); u2.setLogin("active1");   u2.setStatus(UserStatus.ACTIVE);
        User u3 = new User(); u3.setLogin("banned1");   u3.setStatus(UserStatus.BANNED);

        when(userRepository.findByStatus(UserStatus.INACTIVE)).thenReturn(List.of(u1));
        when(userRepository.findByStatus(UserStatus.ACTIVE)).thenReturn(List.of(u2));
        when(userRepository.findByStatus(UserStatus.BANNED)).thenReturn(List.of(u3));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("inactiveUsers", List.of(u1)))
                .andExpect(model().attribute("activeUsers", List.of(u2)))
                .andExpect(model().attribute("bannedUsers", List.of(u3)))
                .andExpect(model().attribute("activeNav", "admin-users"));

        verify(userRepository).findByStatus(UserStatus.INACTIVE);
        verify(userRepository).findByStatus(UserStatus.ACTIVE);
        verify(userRepository).findByStatus(UserStatus.BANNED);
        verifyNoMoreInteractions(userRepository);
    }

    // --- POST /{id}/activate ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void activate_ShouldCallService_AndRedirectWithQueryParam() throws Exception {
        mockMvc.perform(post("/admin/users/10/activate").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?activated"));

        verify(userAdminService).activateUser(10L);
        verifyNoMoreInteractions(userAdminService);
    }

    // --- POST /{id}/ban ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void ban_ShouldCallService_AndRedirectWithQueryParam_WhenOk() throws Exception {
        mockMvc.perform(post("/admin/users/11/ban").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?banned"));

        verify(userAdminService).banUser(11L);
        verifyNoMoreInteractions(userAdminService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void ban_ShouldRedirectWithFlashError_WhenIllegalArgument() throws Exception {
        doThrow(new IllegalArgumentException("Nie można zablokować własnego konta."))
                .when(userAdminService).banUser(11L);

        mockMvc.perform(post("/admin/users/11/ban").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("errorMessage", "Nie można zablokować własnego konta."));

        verify(userAdminService).banUser(11L);
        verifyNoMoreInteractions(userAdminService);
    }

    // --- POST /{id}/unban ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unban_ShouldCallService_AndRedirectWithQueryParam() throws Exception {
        mockMvc.perform(post("/admin/users/12/unban").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?unbanned"));

        verify(userAdminService).unbanUser(12L);
        verifyNoMoreInteractions(userAdminService);
    }

    // --- GET /{id}/reset-password ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void resetPasswordForm_ShouldReturnViewAndUser_ForAdmin() throws Exception {
        User target = new User();
        ReflectionTestUtils.setField(target, "id", 20L);
        target.setLogin("janek");

        when(userRepository.findById(20L)).thenReturn(Optional.of(target));

        mockMvc.perform(get("/admin/users/20/reset-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-reset-password"))
                .andExpect(model().attribute("user", target))
                .andExpect(model().attribute("activeNav", "admin-users"));

        verify(userRepository).findById(20L);
        verifyNoMoreInteractions(userRepository);
    }

    // --- POST /{id}/reset-password ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void resetPassword_ShouldRejectBlankPassword_WithFlashError() throws Exception {
        mockMvc.perform(post("/admin/users/20/reset-password")
                        .with(csrf())
                        .param("newPassword", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/20/reset-password"))
                .andExpect(flash().attribute("errorMessage", "Hasło nie może być puste."));

        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void resetPassword_ShouldCallAuthService_AndRedirectWithSuccessFlash() throws Exception {
        mockMvc.perform(post("/admin/users/20/reset-password")
                        .with(csrf())
                        .param("newPassword", "NoweHaslo123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("successMessage",
                        "Hasło zostało zresetowane. Przekaż je użytkownikowi."));

        verify(authService).resetPasswordByAdmin(20L, "NoweHaslo123");
        verifyNoMoreInteractions(authService);
    }

    // --- GET /{id}/edit ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editUserForm_ShouldReturnViewAndPopulateHeaderData_ForAdmin() throws Exception {
        User target = new User();
        ReflectionTestUtils.setField(target, "id", 30L);
        target.setLogin("ola");
        target.setFirstName("Ola");
        target.setLastName("Nowak");
        target.setPhone("123");
        target.setAddress("Adres 1");

        when(userRepository.findById(30L)).thenReturn(Optional.of(target));

        mockMvc.perform(get("/admin/users/30/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-edit"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("userId", 30L))
                .andExpect(model().attribute("userLogin", "ola"))
                .andExpect(model().attribute("activeNav", "admin-users"));

        verify(userRepository).findById(30L);
        verifyNoMoreInteractions(userRepository);
    }

    // --- POST /{id}/edit ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUser_ShouldReturnEditView_WhenValidationErrors() throws Exception {
        User target = new User();
        ReflectionTestUtils.setField(target, "id", 31L);
        target.setLogin("piotr");

        when(userRepository.findById(31L)).thenReturn(Optional.of(target));

        // Zakładamy, że EditProfileForm ma walidacje @NotBlank na firstName/lastName itp.
        mockMvc.perform(post("/admin/users/31/edit")
                        .with(csrf())
                        .param("firstName", "")      // błąd
                        .param("lastName", "Kowalski")
                        .param("phone", "123")
                        .param("address", "Adres"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-edit"))
                .andExpect(model().attributeExists("userId"))
                .andExpect(model().attributeExists("userLogin"))
                .andExpect(model().attribute("activeNav", "admin-users"))
                .andExpect(model().attributeHasFieldErrors("form", "firstName"));

        verify(userRepository).findById(31L);
        verifyNoInteractions(userAdminService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUser_ShouldCallService_AndRedirectWithSuccessFlash_WhenOk() throws Exception {
        mockMvc.perform(post("/admin/users/32/edit")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Kowalski")
                        .param("phone", "123456789")
                        .param("address", "Ulica 1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("successMessage",
                        "Dane użytkownika zostały zaktualizowane."));

        ArgumentCaptor<EditProfileForm> captor = ArgumentCaptor.forClass(EditProfileForm.class);
        verify(userAdminService).updateUser(eq(32L), captor.capture());

        EditProfileForm sent = captor.getValue();
        assertEquals("Jan", sent.getFirstName());
        assertEquals("Kowalski", sent.getLastName());
        assertEquals("123456789", sent.getPhone());
        assertEquals("Ulica 1", sent.getAddress());

        verifyNoMoreInteractions(userAdminService);
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
