package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.service.UserAdminService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
public class AdminUsersController {

    private final UserRepository userRepository;
    private final UserAdminService userAdminService;

    public AdminUsersController(UserRepository userRepository, UserAdminService userAdminService) {
        this.userRepository = userRepository;
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public String users(Model model) {
        var sort = Sort.by(Sort.Direction.ASC, "createdAt");
        model.addAttribute("inactiveUsers", userRepository.findByStatus(UserStatus.INACTIVE));
        model.addAttribute("activeUsers", userRepository.findByStatus(UserStatus.ACTIVE));
        model.addAttribute("bannedUsers", userRepository.findByStatus(UserStatus.BANNED));
        model.addAttribute("activeNav", "admin-users");

        return "admin/users";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable("id") long id) {
        userAdminService.activateUser(id);
        return "redirect:/admin/users?activated";
    }

    @PostMapping("/{id}/ban")
    public String ban(@PathVariable("id") long id, RedirectAttributes ra) {
        try {
            userAdminService.banUser(id);
            return "redirect:/admin/users?banned";
        } catch (IllegalArgumentException e) {
            // Przekazujemy komunikat błędu do widoku
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/{id}/unban")
    public String unban(@PathVariable("id") long id) {
        userAdminService.unbanUser(id);
        return "redirect:/admin/users?unbanned";
    }
}
