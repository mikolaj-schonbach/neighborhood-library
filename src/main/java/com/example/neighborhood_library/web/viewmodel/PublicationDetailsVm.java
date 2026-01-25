package com.example.neighborhood_library.web.viewmodel;

import java.util.List;

public record PublicationDetailsVm(
        long id,
        String title,
        String kindLabel,
        String authors,
        String isbn,
        Short year,
        String categoryName,
        List<CopyVm> copies
) {}
