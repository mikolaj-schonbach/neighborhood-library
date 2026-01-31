package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.AuthorRepository;
import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.repo.CopyRepository;
import com.example.neighborhood_library.repo.PublicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPublicationServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private PublicationRepository publicationRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private CopyRepository copyRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private OperationService operationService;

    @InjectMocks
    private AdminPublicationService service;

    // --- Create Publication ---

    @Test
    void createPublicationWithOneCopy_ShouldSavePublicationAndAuthorAndCopy() {
        // given
        long categoryId = 1L;
        String authorsRaw = "John Doe; Jane Smith";

        Category category = new Category();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // Mock save for publication (to return ID needed for Copy inventory code)
        when(publicationRepository.save(any(Publication.class))).thenAnswer(invocation -> {
            Publication p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 100L); // simulate DB id
            return p;
        });

        // Mock authors (simulate they don't exist yet)
        when(authorRepository.findByFirstNameAndLastName(anyString(), anyString())).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenAnswer(i -> i.getArgument(0));

        // Mock copy save
        when(copyRepository.save(any(Copy.class))).thenAnswer(invocation -> {
            Copy c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 500L);
            return c;
        });

        // when
        long resultId = service.createPublicationWithOneCopy(
                "  Clean Code  ", authorsRaw, PublicationKind.BOOK, categoryId, " 978-3-16-148410-0 ", (short) 2008
        );

        // then
        assertEquals(100L, resultId);

        // 1. Verify Publication
        ArgumentCaptor<Publication> pubCaptor = ArgumentCaptor.forClass(Publication.class);
        // save is called twice: once initially, once after adding authors
        verify(publicationRepository, times(2)).save(pubCaptor.capture());
        Publication savedPub = pubCaptor.getValue();
        assertEquals("Clean Code", savedPub.getTitle());
        assertEquals("978-3-16-148410-0", savedPub.getIsbn());
        assertEquals(category, savedPub.getCategory());
        // Check if authors were added (we mocked them to be created)
        assertEquals(2, savedPub.getPublicationAuthors().size());

        // 2. Verify Authors creation
        verify(authorRepository).save(argThat(a -> a.getFirstName().equals("John") && a.getLastName().equals("Doe")));
        verify(authorRepository).save(argThat(a -> a.getFirstName().equals("Jane") && a.getLastName().equals("Smith")));

        // 3. Verify Copy creation and inventory code update
        ArgumentCaptor<Copy> copyCaptor = ArgumentCaptor.forClass(Copy.class);
        verify(copyRepository, times(2)).save(copyCaptor.capture());
        Copy savedCopy = copyCaptor.getValue();
        // The final save should have the correct code
        assertTrue(savedCopy.getInventoryCode().startsWith("LIB-"));
        assertTrue(savedCopy.getInventoryCode().endsWith("-000500"));
        assertEquals(CopyStatus.AVAILABLE, savedCopy.getStatus());
    }

    @Test
    void createPublication_ShouldThrowException_WhenAuthorFormatIsInvalid() {
        // given
        long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(new Category()));
        // Note: publicationRepository.save() will be called before parsing authors in current impl,
        // so we might need lenient() or just expect it.
        lenient().when(publicationRepository.save(any())).thenReturn(new Publication());

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.createPublicationWithOneCopy(
                        "Title", "John", PublicationKind.BOOK, categoryId, null, null
                )
        );
        assertTrue(ex.getMessage().contains("Niepoprawny autor"));
    }

    // --- Edit Publication ---

    @Test
    void editPublication_ShouldUpdateDetailsAndResetAuthors() {
        // given
        Long pubId = 10L;
        Long categoryId = 2L;
        String newAuthors = "Uncle Bob";

        Publication existingPub = new Publication();
        ReflectionTestUtils.setField(existingPub, "id", pubId);

        Category newCategory = new Category();

        when(publicationRepository.findByIdWithDetails(pubId)).thenReturn(Optional.of(existingPub));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(newCategory));
        when(currentUserService.requireCurrentUser()).thenReturn(new User());

        // Mock existing author check
        when(authorRepository.findByFirstNameAndLastName("Uncle", "Bob")).thenReturn(Optional.of(new Author()));

        // when
        service.editPublication(pubId, "New Title", newAuthors, PublicationKind.MAGAZINE, categoryId, "", (short) 2024);

        // then
        assertEquals("New Title", existingPub.getTitle());
        assertEquals(PublicationKind.MAGAZINE, existingPub.getKind());
        assertEquals(newCategory, existingPub.getCategory());
        assertNull(existingPub.getIsbn()); // empty string -> null
        assertEquals((short) 2024, existingPub.getYear());

        // Authors cleared and re-added
        assertEquals(1, existingPub.getPublicationAuthors().size());

        verify(publicationRepository).save(existingPub);
        verify(operationService).logAction(any(), isNull(), eq("PUBLICATION_UPDATED"), isNull());
    }

    // --- Add Copy ---

    @Test
    void addCopy_ShouldCreateNewCopyForPublication() {
        // given
        Long pubId = 10L;
        Publication p = new Publication();
        when(publicationRepository.findById(pubId)).thenReturn(Optional.of(p));
        when(currentUserService.requireCurrentUser()).thenReturn(new User());

        when(copyRepository.save(any(Copy.class))).thenAnswer(i -> {
            Copy c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 999L);
            return c;
        });

        // when
        service.addCopy(pubId);

        // then
        ArgumentCaptor<Copy> captor = ArgumentCaptor.forClass(Copy.class);
        verify(copyRepository, times(2)).save(captor.capture());
        Copy finalCopy = captor.getValue();

        assertEquals(p, finalCopy.getPublication());
        assertEquals(CopyStatus.AVAILABLE, finalCopy.getStatus());
        assertTrue(finalCopy.getInventoryCode().contains("-000999"));

        verify(operationService).logAction(any(), isNull(), eq("COPY_CREATED"), any());
    }

    // --- Delete Copy ---

    @Test
    void deleteCopy_ShouldSoftDelete_WhenStatusIsAvailable() {
        // given
        Long copyId = 55L;
        Copy copy = new Copy();
        copy.setStatus(CopyStatus.AVAILABLE);

        when(copyRepository.findById(copyId)).thenReturn(Optional.of(copy));
        when(currentUserService.requireCurrentUser()).thenReturn(new User());

        // when
        service.deleteCopy(copyId);

        // then
        assertEquals(CopyStatus.UNAVAILABLE, copy.getStatus());
        assertNotNull(copy.getDeletedAt());
        verify(copyRepository).save(copy);
        verify(operationService).logAction(any(), isNull(), eq("COPY_DELETED"), eq(copy));
    }

    @Test
    void deleteCopy_ShouldThrowException_WhenCopyIsLoaned() {
        // given
        Long copyId = 55L;
        Copy copy = new Copy();
        copy.setStatus(CopyStatus.LOANED);

        when(copyRepository.findById(copyId)).thenReturn(Optional.of(copy));

        // when & then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.deleteCopy(copyId)
        );
        assertEquals("Nie można usunąć egzemplarza, który jest wypożyczony lub zarezerwowany.", ex.getMessage());

        verify(copyRepository, never()).save(any());
    }
}
