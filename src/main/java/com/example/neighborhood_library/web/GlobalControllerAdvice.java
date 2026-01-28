package com.example.neighborhood_library.web;

import com.example.neighborhood_library.service.LibraryInfoService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final LibraryInfoService libraryInfoService;

    public GlobalControllerAdvice(LibraryInfoService libraryInfoService) {
        this.libraryInfoService = libraryInfoService;
    }

    // Dzięki temu obiekt "libraryInfo" będzie dostępny w KAŻDYM widoku Thymeleaf
    // Pozwala to umieścić modal z informacjami w głównym layout.html
    @ModelAttribute("libraryInfo")
    public com.example.neighborhood_library.domain.LibraryInfo globalLibraryInfo() {
        return libraryInfoService.getInfo();
    }
}