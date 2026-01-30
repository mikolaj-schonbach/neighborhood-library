package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.PublicationRepository;
import com.example.neighborhood_library.web.viewmodel.PublicationDetailsVm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private PublicationRepository publicationRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void getDetails_ShouldMapEntityToViewModelCorrectly() {
        // given
        long pubId = 10L;

        Category cat = new Category();
        cat.setName("IT");

        Author a1 = new Author(); a1.setFirstName("Adam"); a1.setLastName("Mickiewicz");
        Author a2 = new Author(); a2.setFirstName("Juliusz"); a2.setLastName("Słowacki");

        Publication p = new Publication();
        ReflectionTestUtils.setField(p, "id", pubId);
        p.setTitle("Wielka Improwizacja");
        p.setKind(PublicationKind.BOOK);
        p.setYear((short) 1832);
        p.setIsbn("123-456");
        p.setCategory(cat);

        p.addAuthor(a1);
        p.addAuthor(a2);

        Copy c1 = new Copy(); c1.setInventoryCode("C1"); c1.setStatus(CopyStatus.AVAILABLE);
        Copy c2 = new Copy(); c2.setInventoryCode("C2"); c2.setStatus(CopyStatus.LOANED);
        ReflectionTestUtils.setField(p, "copies", Set.of(c1, c2));

        when(publicationRepository.findByIdWithDetails(pubId)).thenReturn(Optional.of(p));

        // when
        PublicationDetailsVm details = catalogService.getDetails(pubId);

        // then
        assertEquals("Wielka Improwizacja", details.title());

        // Dostosowanie do prawdopodobnych nazw pól w rekordzie
        assertEquals("Książka", details.kindLabel());
        assertEquals("IT", details.categoryName());

        // Sprawdzamy formatowanie autorów (sortowanie alfabetyczne i przecinki)
        assertTrue(details.authors().contains("Adam Mickiewicz"));
        assertTrue(details.authors().contains("Juliusz Słowacki"));

        assertEquals(2, details.copies().size());
    }
}