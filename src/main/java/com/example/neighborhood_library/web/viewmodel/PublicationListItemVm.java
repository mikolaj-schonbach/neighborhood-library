package com.example.neighborhood_library.web.viewmodel;

public record PublicationListItemVm(
        long id,
        String title,
        String kindLabel,
        String authors,
        String categoryName,
        long available,
        long reserved,
        long loaned,
        long unavailable
) {}
