package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.PublicationRepository;
import com.example.neighborhood_library.support.NotFoundException;
import com.example.neighborhood_library.web.viewmodel.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final PublicationRepository publicationRepository;

    public CatalogService(PublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    @Transactional(readOnly = true)
    public Page<PublicationListItemVm> search(String q, Long categoryId, int page1Based, int size) {
        String query = normalize(q);

        int page0 = Math.max(0, page1Based - 1);
        Pageable pageable = PageRequest.of(page0, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Long> ids = publicationRepository.searchIds(query, categoryId, pageable);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, ids.getTotalElements());
        }

        List<Long> idList = ids.getContent();
        List<Publication> pubs = publicationRepository.findAllByIdInWithDetails(idList);

        Map<Long, Publication> byId = pubs.stream()
                .collect(Collectors.toMap(Publication::getId, Function.identity()));

        List<PublicationListItemVm> items = new ArrayList<>(idList.size());
        for (Long id : idList) {
            Publication p = byId.get(id);
            if (p != null) items.add(toListItemVm(p));
        }

        return new PageImpl<>(items, pageable, ids.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PublicationDetailsVm getDetails(long publicationId) {
        Publication p = publicationRepository.findByIdWithDetails(publicationId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono publikacji id=" + publicationId));

        List<CopyVm> copies = p.getCopies().stream()
                .filter(c -> c.getDeletedAt() == null)
                .sorted(Comparator.comparing(Copy::getInventoryCode))
                .map(c -> new CopyVm(c.getInventoryCode(), copyStatusLabel(c.getStatus())))
                .toList();

        return new PublicationDetailsVm(
                p.getId(),
                p.getTitle(),
                kindLabel(p.getKind()),
                authorsLabel(p),
                p.getIsbn(),
                p.getYear(),
                p.getCategory().getName(),
                copies
        );
    }

    private PublicationListItemVm toListItemVm(Publication p) {
        Map<CopyStatus, Long> counts = p.getCopies().stream()
                .filter(c -> c.getDeletedAt() == null)
                .collect(Collectors.groupingBy(Copy::getStatus, Collectors.counting()));

        long available = counts.getOrDefault(CopyStatus.AVAILABLE, 0L);
        long reserved = counts.getOrDefault(CopyStatus.RESERVED, 0L);
        long loaned = counts.getOrDefault(CopyStatus.LOANED, 0L);
        long unavailable = counts.getOrDefault(CopyStatus.UNAVAILABLE, 0L);

        return new PublicationListItemVm(
                p.getId(),
                p.getTitle(),
                kindLabel(p.getKind()),
                authorsLabel(p),
                p.getCategory().getName(),
                available,
                reserved,
                loaned,
                unavailable
        );
    }

    private String authorsLabel(Publication p) {
        // PublicationAuthor -> Author
        return p.getPublicationAuthors().stream()
                .map(PublicationAuthor::getAuthor)
                .map(a -> (a.getFirstName() + " " + a.getLastName()).trim())
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private String kindLabel(PublicationKind kind) {
        return switch (kind) {
            case BOOK -> "Książka";
            case MAGAZINE -> "Czasopismo";
        };
    }

    private String copyStatusLabel(CopyStatus status) {
        return switch (status) {
            case AVAILABLE -> "Dostępna";
            case RESERVED -> "Zarezerwowana";
            case LOANED -> "Wypożyczona";
            case UNAVAILABLE -> "Niedostępna";
        };
    }

    private String normalize(String q) {
        if (q == null) return null;
        String t = q.trim();
        return t.isBlank() ? null : t;
    }
}
