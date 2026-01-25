package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.*;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminPublicationService {

    private final CategoryRepository categoryRepository;
    private final PublicationRepository publicationRepository;
    private final AuthorRepository authorRepository;
    private final CopyRepository copyRepository;

    public AdminPublicationService(CategoryRepository categoryRepository,
                                   PublicationRepository publicationRepository,
                                   AuthorRepository authorRepository,
                                   CopyRepository copyRepository) {
        this.categoryRepository = categoryRepository;
        this.publicationRepository = publicationRepository;
        this.authorRepository = authorRepository;
        this.copyRepository = copyRepository;
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
