package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.Publication;
import com.example.neighborhood_library.domain.PublicationAuthor;
import com.example.neighborhood_library.domain.PublicationKind;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.repo.PublicationRepository;
import com.example.neighborhood_library.service.AdminPublicationService;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminPublicationsController.class)
@Import({SecurityConfig.class, AdminPublicationsControllerTest.LibraryInfoServiceMockConfig.class})
class AdminPublicationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CategoryRepository categoryRepository;
    @MockBean private PublicationRepository publicationRepository;
    @MockBean private AdminPublicationService adminPublicationService;

    // --- Mocki dla GlobalControllerAdvice ---
    @MockBean private MessageService messageService;
    @MockBean private CurrentUserService currentUserService;

    @Autowired
    private LibraryInfoService libraryInfoService; // bean z TestConfiguration (żeby Thymeleaf nie dostawał null)
    // ---------------------------------------

    @BeforeEach
    void setUp() {
        User adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        adminUser.setLogin("admin");
        adminUser.setAccountRole(AccountRole.ADMIN);

        when(currentUserService.requireCurrentUser()).thenReturn(adminUser);
        when(categoryRepository.findAll(org.mockito.ArgumentMatchers.any(Sort.class))).thenReturn(List.of());
    }

    // --- SECURITY CHECKS ---

    @Test
    void newForm_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/admin/publications/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void newForm_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/publications/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void create_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(post("/admin/publications").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- GET /new ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void newForm_ShouldReturnViewAndPopulateModel_ForAdmin() throws Exception {
        mockMvc.perform(get("/admin/publications/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/publication-new"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attributeExists("kinds"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("activeNav", "admin-publications"));

        verify(categoryRepository).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    // --- POST /admin/publications (create) ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_ShouldRedirectToCatalog_WithSuccessFlash_WhenOk() throws Exception {
        when(adminPublicationService.createPublicationWithOneCopy(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(PublicationKind.class),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Short.class) // w Twoim projekcie to najpewniej Short/short
        )).thenReturn(42L);

        mockMvc.perform(post("/admin/publications")
                        .with(csrf())
                        .param("title", "Testowa publikacja")
                        .param("authors", "Jan Kowalski")
                        .param("kind", PublicationKind.values()[0].name())
                        .param("categoryId", "1")
                        .param("isbn", "9780306406157")
                        .param("year", "2020"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalog/42"))
                .andExpect(flash().attribute("successMessage", "Dodano publikację i 1 egzemplarz."));

        verify(adminPublicationService).createPublicationWithOneCopy(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(PublicationKind.class),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Short.class)
        );
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_ShouldReturnForm_WhenValidationErrors() throws Exception {
        mockMvc.perform(post("/admin/publications")
                        .with(csrf())
                        .param("title", "")) // typowo @NotBlank
                .andExpect(status().isOk())
                .andExpect(view().name("admin/publication-new"))
                .andExpect(model().attributeExists("kinds"))
                .andExpect(model().attributeExists("categories"));

        verifyNoInteractions(adminPublicationService);
        verify(categoryRepository, atLeastOnce()).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_ShouldReturnForm_WithErrorMessage_WhenIllegalArgument() throws Exception {
        when(adminPublicationService.createPublicationWithOneCopy(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(PublicationKind.class),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Short.class)
        )).thenThrow(new IllegalArgumentException("Zły ISBN"));

        mockMvc.perform(post("/admin/publications")
                        .with(csrf())
                        .param("title", "Testowa publikacja")
                        .param("authors", "Jan Kowalski")
                        .param("kind", PublicationKind.values()[0].name())
                        .param("categoryId", "1")
                        .param("isbn", "9780306406157") // dajemy poprawny, żeby nie zatrzymała walidacja
                        .param("year", "2020"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/publication-new"))
                .andExpect(model().attribute("errorMessage", "Zły ISBN"))
                .andExpect(model().attributeExists("kinds"))
                .andExpect(model().attributeExists("categories"));

        verify(categoryRepository, atLeastOnce()).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_ShouldReturnForm_WithGenericErrorMessage_WhenException() throws Exception {
        when(adminPublicationService.createPublicationWithOneCopy(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(PublicationKind.class),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Short.class)
        )).thenThrow(new RuntimeException("Boom"));

        mockMvc.perform(post("/admin/publications")
                        .with(csrf())
                        .param("title", "Testowa publikacja")
                        .param("authors", "Jan Kowalski")
                        .param("kind", PublicationKind.values()[0].name())
                        .param("categoryId", "1")
                        .param("isbn", "9780306406157")
                        .param("year", "2020"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/publication-new"))
                .andExpect(model().attribute("errorMessage",
                        "Nie udało się dodać publikacji (sprawdź ISBN / duplikaty)."))
                .andExpect(model().attributeExists("kinds"))
                .andExpect(model().attributeExists("categories"));

        verify(categoryRepository, atLeastOnce()).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    // --- GET /{id}/edit ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void editForm_ShouldReturnView_AndBuildFormFromPublication_WhenNoFormInModel() throws Exception {
        long pubId = 5L;
        PublicationKind kind = PublicationKind.values()[0];

        Publication p = mock(Publication.class, RETURNS_DEEP_STUBS);
        when(p.getTitle()).thenReturn("Tytuł");
        when(p.getKind()).thenReturn(kind);
        when(p.getCategory().getId()).thenReturn(7L);
        when(p.getIsbn()).thenReturn("9780306406157");
        when(p.getYear()).thenReturn((short) 2020);

        PublicationAuthor pa1 = mock(PublicationAuthor.class, RETURNS_DEEP_STUBS);
        when(pa1.getAuthor().getFirstName()).thenReturn("Jan");
        when(pa1.getAuthor().getLastName()).thenReturn("Kowalski");

        PublicationAuthor pa2 = mock(PublicationAuthor.class, RETURNS_DEEP_STUBS);
        when(pa2.getAuthor().getFirstName()).thenReturn("Anna");
        when(pa2.getAuthor().getLastName()).thenReturn("Nowak");

        Set<PublicationAuthor> ordered = new LinkedHashSet<>();
        ordered.add(pa1);
        ordered.add(pa2);
        when(p.getPublicationAuthors()).thenReturn(ordered);

        when(publicationRepository.findByIdWithDetails(pubId)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/admin/publications/{id}/edit", pubId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/publication-edit"))
                .andExpect(model().attribute("activeNav", "admin-publications"))
                .andExpect(model().attributeExists("publication"))
                .andExpect(model().attributeExists("kinds"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("form", allOf(
                        hasProperty("title", is("Tytuł")),
                        hasProperty("kind", is(kind)),
                        hasProperty("categoryId", is(7L)),
                        hasProperty("isbn", is("9780306406157")),
                        hasProperty("authors", is("Jan Kowalski; Anna Nowak"))
                )));

        verify(publicationRepository).findByIdWithDetails(pubId);
        verify(categoryRepository).findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    // --- POST /{id}/edit (update) ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_ShouldReturnEditView_WhenValidationErrors() throws Exception {
        long pubId = 5L;

        Publication p = mock(Publication.class, RETURNS_DEEP_STUBS);
        when(publicationRepository.findByIdWithDetails(pubId)).thenReturn(Optional.of(p));

        mockMvc.perform(post("/admin/publications/{id}/edit", pubId)
                        .with(csrf())
                        .param("title", "") // wymusi błędy walidacji
                        .param("authors", "Jan Kowalski")
                        .param("kind", PublicationKind.values()[0].name())
                        .param("categoryId", "1")
                        .param("isbn", "9780306406157")
                        .param("year", "2020"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/publication-edit"))
                .andExpect(model().attributeExists("publication"))
                .andExpect(model().attributeExists("kinds"))
                .andExpect(model().attributeExists("categories"));

        verify(publicationRepository).findByIdWithDetails(pubId);
        verifyNoMoreInteractions(adminPublicationService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_ShouldRedirectWithSuccessFlash_WhenOk() throws Exception {
        long pubId = 5L;

        mockMvc.perform(post("/admin/publications/{id}/edit", pubId)
                        .with(csrf())
                        .param("title", "Nowy tytuł")
                        .param("authors", "Jan Kowalski")
                        .param("kind", PublicationKind.values()[0].name())
                        .param("categoryId", "1")
                        .param("isbn", "9780306406157")
                        .param("year", "2020"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/publications/5/edit"))
                .andExpect(flash().attribute("successMessage", "Zapisano zmiany."));

        verify(adminPublicationService).editPublication(
                eq(5L),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(PublicationKind.class),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Short.class)
        );
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_ShouldRedirectWithErrorFlash_WhenServiceThrows() throws Exception {
        doThrow(new IllegalStateException("Błąd zapisu"))
                .when(adminPublicationService)
                .editPublication(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(PublicationKind.class),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(Short.class)
                );

        mockMvc.perform(post("/admin/publications/{id}/edit", 5L)
                        .with(csrf())
                        .param("title", "Nowy tytuł")
                        .param("authors", "Jan Kowalski")
                        .param("kind", PublicationKind.values()[0].name())
                        .param("categoryId", "1")
                        .param("isbn", "9780306406157")
                        .param("year", "2020"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/publications/5/edit"))
                .andExpect(flash().attribute("errorMessage", "Błąd zapisu"));
    }

    // --- copies/add ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void addCopy_ShouldRedirectWithSuccessFlash_WhenOk() throws Exception {
        mockMvc.perform(post("/admin/publications/5/copies/add").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/publications/5/edit"))
                .andExpect(flash().attribute("successMessage", "Dodano nowy egzemplarz."));

        verify(adminPublicationService).addCopy(5L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void addCopy_ShouldRedirectWithErrorFlash_WhenServiceThrows() throws Exception {
        doThrow(new IllegalStateException("Nie można"))
                .when(adminPublicationService).addCopy(5L);

        mockMvc.perform(post("/admin/publications/5/copies/add").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/publications/5/edit"))
                .andExpect(flash().attribute("errorMessage", "Błąd: Nie można"));
    }

    // --- copies/{copyId}/delete ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteCopy_ShouldRedirectWithSuccessFlash_WhenOk() throws Exception {
        mockMvc.perform(post("/admin/publications/5/copies/99/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/publications/5/edit"))
                .andExpect(flash().attribute("successMessage", "Egzemplarz usunięty (soft delete)."));

        verify(adminPublicationService).deleteCopy(99L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteCopy_ShouldRedirectWithErrorFlash_WhenServiceThrows() throws Exception {
        doThrow(new IllegalStateException("Wypożyczony"))
                .when(adminPublicationService).deleteCopy(99L);

        mockMvc.perform(post("/admin/publications/5/copies/99/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/publications/5/edit"))
                .andExpect(flash().attribute("errorMessage", "Nie można usunąć: Wypożyczony"));
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
