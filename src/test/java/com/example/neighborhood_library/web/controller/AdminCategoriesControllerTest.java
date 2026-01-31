package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.service.AdminCategoryService;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCategoriesController.class)
@Import(SecurityConfig.class)
class AdminCategoriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AdminCategoryService adminCategoryService;
    @MockBean private CategoryRepository categoryRepository;
    @MockBean private LibraryInfoService libraryInfoService;
    @MockBean private MessageService messageService;
    @MockBean private CurrentUserService currentUserService;

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
    @WithMockUser(username = "zwyklyUser", roles = "USER")
    void index_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void index_ShouldAllowAccess_ForAdmin() throws Exception {
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories")); // POPRAWKA: nazwa widoku
    }

    // --- ACTIONS ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_ShouldCallServiceAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .param("name", "Nowa Kategoria"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage")); // POPRAWKA: nazwa atrybutu

        verify(adminCategoryService).create("Nowa Kategoria");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_ShouldCallServiceAndRedirect() throws Exception {
        mockMvc.perform(post("/admin/categories/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage")); // POPRAWKA: nazwa atrybutu

        verify(adminCategoryService).delete(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_ShouldHandleError_WhenCategoryIsUsed() throws Exception {
        // given
        doThrow(new IllegalStateException("Nie można usunąć kategorii..."))
                .when(adminCategoryService).delete(1L);

        // when
        mockMvc.perform(post("/admin/categories/1/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("errorMessage")); // POPRAWKA: nazwa atrybutu
    }
}