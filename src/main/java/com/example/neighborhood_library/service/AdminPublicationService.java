package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.*;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminPublicationService {

    private final CategoryRepository categoryRepository;
    private final PublicationRepository publicationRepository;
    private final AuthorRepository authorRepository;
    private final CopyRepository copyRepository;
    private final CurrentUserService currentUserService;
    private final OperationService operationService;

    public AdminPublicationService(CategoryRepository categoryRepository,
                                   PublicationRepository publicationRepository,
                                   AuthorRepository authorRepository,
                                   CopyRepository copyRepository, CurrentUserService currentUserService, OperationService operationService) {
        this.categoryRepository = categoryRepository;
        this.publicationRepository = publicationRepository;
        this.authorRepository = authorRepository;
        this.copyRepository = copyRepository;
        this.currentUserService = currentUserService;
        this.operationService = operationService;
    }

    @Transactional
    public long createPublicationWithOneCopy(String title,
                                             String authorsRaw,
                                             PublicationKind kind,
                                             long categoryId,
                                             String isbn,
                                             Short year) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono kategorii id=" + categoryId));

        Publication p = new Publication();
        p.setTitle(title.trim());
        p.setKind(kind);
        p.setCategory(category);

        String isbnNorm = (isbn == null) ? null : isbn.trim();
        if (isbnNorm != null && isbnNorm.isBlank()) isbnNorm = null;
        p.setIsbn(isbnNorm);

        p.setYear(year);

        // najpierw zapis publikacji, żeby miała id
        Publication saved = publicationRepository.save(p);

        // autorzy
        List<AuthorName> names = parseAuthors(authorsRaw);
        List<Author> authors = new ArrayList<>();
        for (AuthorName n : names) {
            Author a = authorRepository.findByFirstNameAndLastName(n.firstName(), n.lastName())
                    .orElseGet(() -> {
                        Author created = new Author();
                        created.setFirstName(n.firstName());
                        created.setLastName(n.lastName());
                        return authorRepository.save(created);
                    });
            authors.add(a);
        }

        // połącz
        for (Author a : authors) {
            saved.addAuthor(a);
        }
        publicationRepository.save(saved);

        // egzemplarz: najpierw tymczasowy inventory_code, potem docelowy LIB-YYYY-NNNNNN
        Copy copy = new Copy();
        copy.setPublication(saved);
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setInventoryCode("TMP-" + UUID.randomUUID());
        Copy savedCopy = copyRepository.save(copy);

        savedCopy.setInventoryCode(generateInventoryCode(savedCopy.getId()));
        copyRepository.save(savedCopy);

        return saved.getId();
    }

    @Transactional
    public void editPublication(Long publicationId, String title, String authorsRaw,
                                PublicationKind kind, Long categoryId, String isbn, Short year) {

        Publication p = publicationRepository.findByIdWithDetails(publicationId)
                .orElseThrow(() -> new NotFoundException("Publikacja nie istnieje"));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Kategoria nie istnieje"));

        p.setTitle(title.trim());
        p.setKind(kind);
        p.setCategory(category);

        String isbnNorm = (isbn == null) ? null : isbn.trim();
        if (isbnNorm != null && isbnNorm.isBlank()) isbnNorm = null;
        p.setIsbn(isbnNorm);

        p.setYear(year);

        // Aktualizacja autorów (uproszczona: czyścimy i dodajemy na nowo)
        // W produkcyjnym kodzie lepiej byłoby sprawdzać diff, żeby nie usuwać ID
        p.getPublicationAuthors().clear();

        List<AuthorName> names = parseAuthors(authorsRaw);
        for (AuthorName n : names) {
            Author a = authorRepository.findByFirstNameAndLastName(n.firstName(), n.lastName())
                    .orElseGet(() -> {
                        Author created = new Author();
                        created.setFirstName(n.firstName());
                        created.setLastName(n.lastName());
                        return authorRepository.save(created);
                    });
            p.addAuthor(a);
        }

        publicationRepository.save(p);

        // Log
        operationService.logAction(currentUserService.requireCurrentUser(), null, "PUBLICATION_UPDATED", null);
    }

    @Transactional
    public void addCopy(Long publicationId) {
        Publication p = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new NotFoundException("Publikacja nie istnieje"));

        Copy copy = new Copy();
        copy.setPublication(p);
        copy.setStatus(CopyStatus.AVAILABLE);
        // Tymczasowy kod, nadpisany zaraz po save
        copy.setInventoryCode("TMP-" + UUID.randomUUID());

        Copy saved = copyRepository.save(copy);
        saved.setInventoryCode(generateInventoryCode(saved.getId()));
        copyRepository.save(saved);

        operationService.logAction(currentUserService.requireCurrentUser(), null, "COPY_CREATED", saved);
    }

    @Transactional
    public void deleteCopy(Long copyId) {
        Copy copy = copyRepository.findById(copyId)
                .orElseThrow(() -> new NotFoundException("Egzemplarz nie istnieje"));

        // Reguła biznesowa: nie usuwamy, jeśli wypożyczona lub zarezerwowana
        if (copy.getStatus() == CopyStatus.LOANED || copy.getStatus() == CopyStatus.RESERVED) {
            throw new IllegalStateException("Nie można usunąć egzemplarza, który jest wypożyczony lub zarezerwowany.");
        }

        // Soft delete
        copy.setDeletedAt(OffsetDateTime.now());
        copy.setStatus(CopyStatus.UNAVAILABLE);
        copyRepository.save(copy);

        operationService.logAction(currentUserService.requireCurrentUser(), null, "COPY_DELETED", copy);
    }


    private String generateInventoryCode(long copyId) {
        int yyyy = Year.now().getValue();
        return "LIB-" + yyyy + "-" + String.format("%06d", copyId);
    }

    private List<AuthorName> parseAuthors(String raw) {
        if (raw == null || raw.trim().isBlank()) {
            throw new IllegalArgumentException("Pole Autorzy jest wymagane.");
        }

        // separator: średnik (w PRD najczytelniej); dopuszczamy też przecinki
        String normalized = raw.replace(",", ";");
        List<String> parts = Arrays.stream(normalized.split(";"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Podaj co najmniej jednego autora.");
        }

        List<AuthorName> result = new ArrayList<>();
        for (String p : parts) {
            List<String> tokens = Arrays.stream(p.split("\\s+"))
                    .filter(t -> !t.isBlank())
                    .toList();
            if (tokens.size() < 2) {
                throw new IllegalArgumentException("Niepoprawny autor: '" + p + "'. Użyj formatu: Imię Nazwisko; Imię Nazwisko");
            }
            String lastName = tokens.get(tokens.size() - 1);
            String firstName = String.join(" ", tokens.subList(0, tokens.size() - 1));
            result.add(new AuthorName(firstName, lastName));
        }

        // usuń duplikaty
        return result.stream().distinct().collect(Collectors.toList());
    }

    private record AuthorName(String firstName, String lastName) {}
}
