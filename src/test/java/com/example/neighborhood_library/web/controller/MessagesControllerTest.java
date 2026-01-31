package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
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

@WebMvcTest(MessagesController.class)
@Import({SecurityConfig.class, MessagesControllerTest.LibraryInfoServiceMockConfig.class})
class MessagesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private MessageService messageService;

    // bezpieczny bean (żeby Thymeleaf/layout nie dostawał null)
    @Autowired
    private LibraryInfoService libraryInfoService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        ReflectionTestUtils.setField(user, "id", 100L);
        user.setLogin("user1");

        // GlobalControllerAdvice liczy nieprzeczytane -> unikamy NPE i możemy to weryfikować
        when(messageService.countUnread(100L)).thenReturn(0L);
    }

    // --- SECURITY ---

    @Test
    void index_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/my/messages"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(currentUserService, messageService);
    }

    @Test
    void markAsRead_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(post("/my/messages/10/read").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(currentUserService, messageService);
    }

    @Test
    void delete_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(post("/my/messages/10/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        verifyNoInteractions(currentUserService, messageService);
    }

    // --- GET /my/messages ---

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void index_ShouldReturnViewAndModel_ForUser() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(messageService.getMyMessages(100L)).thenReturn(List.of());

        mockMvc.perform(get("/my/messages"))
                .andExpect(status().isOk())
                .andExpect(view().name("my/messages"))
                .andExpect(model().attribute("activeNav", "messages"))
                .andExpect(model().attribute("messages", List.of()));

        // dodatkowe wywołanie z GlobalControllerAdvice:
        verify(messageService).countUnread(100L);
        verify(messageService).getMyMessages(100L);
        verifyNoMoreInteractions(messageService);
    }

    // --- POST /my/messages/{id}/read ---

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void markAsRead_ShouldCallServiceAndRedirect_ForUser() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);

        mockMvc.perform(post("/my/messages/55/read").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/messages"));

        // dodatkowe wywołanie z GlobalControllerAdvice:
        verify(messageService).countUnread(100L);
        verify(messageService).markAsRead(55L, 100L);
        verifyNoMoreInteractions(messageService);
    }

    // --- POST /my/messages/{id}/delete ---

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void delete_ShouldRedirectWithSuccessFlash_WhenOk() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);

        mockMvc.perform(post("/my/messages/77/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/messages"))
                .andExpect(flash().attribute("success", "Wiadomość usunięta."));

        // dodatkowe wywołanie z GlobalControllerAdvice:
        verify(messageService).countUnread(100L);
        verify(messageService).delete(77L, 100L);
        verifyNoMoreInteractions(messageService);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    void delete_ShouldRedirectWithErrorFlash_WhenServiceThrows() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        doThrow(new RuntimeException("boom")).when(messageService).delete(77L, 100L);

        mockMvc.perform(post("/my/messages/77/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my/messages"))
                .andExpect(flash().attribute("error", "Nie udało się usunąć wiadomości."));

        // dodatkowe wywołanie z GlobalControllerAdvice:
        verify(messageService).countUnread(100L);
        verify(messageService).delete(77L, 100L);
        verifyNoMoreInteractions(messageService);
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
                } catch (Exception ignored) { }

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
