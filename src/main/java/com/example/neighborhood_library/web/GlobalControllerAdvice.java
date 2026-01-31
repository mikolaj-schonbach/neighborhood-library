package com.example.neighborhood_library.web;

import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final LibraryInfoService libraryInfoService;
    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    public GlobalControllerAdvice(LibraryInfoService libraryInfoService, MessageService messageService, CurrentUserService currentUserService) {
        this.libraryInfoService = libraryInfoService;
        this.messageService = messageService;
        this.currentUserService = currentUserService;
    }

    // Dzięki temu obiekt "libraryInfo" będzie dostępny w KAŻDYM widoku Thymeleaf
    // Pozwala to umieścić modal z informacjami w głównym layout.html
    @ModelAttribute("libraryInfo")
    public com.example.neighborhood_library.domain.LibraryInfo globalLibraryInfo() {
        return libraryInfoService.getInfo();
    }

    // Nowa metoda: licznik nieprzeczytanych wiadomości
    @ModelAttribute("unreadMessagesCount")
    public long unreadMessagesCount() {
        try {
            // Próbujemy pobrać zalogowanego użytkownika
            var user = currentUserService.requireCurrentUser();
            return messageService.countUnread(user.getId());
        } catch (NotFoundException | ClassCastException e) {
            // Jeśli użytkownik niezalogowany (anonymousUser) lub błąd
            return 0;
        }
    }
}
