package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.service.CatalogService;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import com.example.neighborhood_library.service.ReservationService;
import com.example.neighborhood_library.web.viewmodel.PublicationDetailsVm;
import com.example.neighborhood_library.web.viewmodel.PublicationListItemVm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser; // <--- Import
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "user", roles = "USER") // <--- KLUCZOWE: Wszyscy wchodzący tu są zalogowani
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;
    @MockitoBean private CategoryRepository categoryRepository;
    @MockitoBean private ReservationService reservationService;

    // --- Boilerplate dla GlobalControllerAdvice ---
    @MockitoBean private LibraryInfoService libraryInfoService;
    @MockitoBean private MessageService messageService;
    @MockitoBean private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        // Ponieważ jesteśmy zalogowani (@WithMockUser), GlobalControllerAdvice może chcieć pobrać usera
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        when(currentUserService.requireCurrentUser()).thenReturn(user);
    }

    @Test
    void index_ShouldReturnCatalogView_WithAttributes() throws Exception {
        // given
        Page<PublicationListItemVm> emptyPage = new PageImpl<>(Collections.emptyList());
        when(catalogService.search(any(), any(), anyInt(), anyInt())).thenReturn(emptyPage);

        when(categoryRepository.findNonEmptyForCatalog()).thenReturn(Collections.emptyList());

        // when
        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalog/index"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    void search_ShouldPassParametersToService() throws Exception {
        // given
        String query = "Harry Potter";
        Long catId = 5L;
        Page<PublicationListItemVm> emptyPage = new PageImpl<>(Collections.emptyList());

        when(catalogService.search(eq(query), eq(catId), anyInt(), anyInt())).thenReturn(emptyPage);

        // when
        mockMvc.perform(get("/catalog")
                        .param("q", query)
                        .param("categoryId", catId.toString()))
                .andExpect(status().isOk());

        // then
        verify(catalogService).search(eq(query), eq(catId), anyInt(), anyInt());
    }

    @Test
    void details_ShouldReturnDetailsView_WhenPublicationExists() throws Exception {
        // given
        Long pubId = 10L;
        PublicationDetailsVm details = new PublicationDetailsVm(
                pubId, "Tytuł", "Książka", "Autor", "ISBN", (short)2020, "Kategoria", List.of()
        );

        when(catalogService.getDetails(pubId)).thenReturn(details);

        // when
        mockMvc.perform(get("/catalog/{id}", pubId))
                .andExpect(status().isOk())
                .andExpect(view().name("catalog/details"))
                .andExpect(model().attribute("publication", details));
    }
}
