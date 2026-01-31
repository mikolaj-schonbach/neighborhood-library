package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.MessageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/my/messages")
public class MessagesController {

    private final CurrentUserService currentUserService;
    private final MessageService messageService;

    public MessagesController(CurrentUserService currentUserService, MessageService messageService) {
        this.currentUserService = currentUserService;
        this.messageService = messageService;
    }

    @GetMapping
    public String index(Model model) {
        User user = currentUserService.requireCurrentUser();
        model.addAttribute("messages", messageService.getMyMessages(user.getId()));
        model.addAttribute("activeNav", "messages");
        return "my/messages";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id) {
        User user = currentUserService.requireCurrentUser();
        messageService.markAsRead(id, user.getId());
        return "redirect:/my/messages";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        User user = currentUserService.requireCurrentUser();
        try {
            messageService.delete(id, user.getId());
            ra.addFlashAttribute("success", "Wiadomość usunięta.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Nie udało się usunąć wiadomości.");
        }
        return "redirect:/my/messages";
    }
}
