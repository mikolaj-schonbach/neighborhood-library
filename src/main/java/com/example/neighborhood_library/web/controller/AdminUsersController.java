package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.service.AuthService;
import com.example.neighborhood_library.service.UserAdminService;
import com.example.neighborhood_library.web.dto.EditProfileForm;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
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
    private final AuthService authService;

    public AdminUsersController(UserRepository userRepository,
                                UserAdminService userAdminService,
                                AuthService authService) {
        this.userRepository = userRepository;
        this.userAdminService = userAdminService;
        this.authService = authService;
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

    // Formularz resetu hasła (GET)
    @GetMapping("/{id}/reset-password")
    public String resetPasswordForm(@PathVariable("id") long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("activeNav", "admin-users");
        return "admin/user-reset-password";
    }

    // Wykonanie resetu (POST)
    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable("id") long id,
                                @RequestParam("newPassword") String newPassword,
                                RedirectAttributes ra) {
        if (newPassword == null || newPassword.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Hasło nie może być puste.");
            return "redirect:/admin/users/" + id + "/reset-password";
        }

        authService.resetPasswordByAdmin(id, newPassword);
        ra.addFlashAttribute("successMessage", "Hasło zostało zresetowane. Przekaż je użytkownikowi.");
        // Wracamy do listy użytkowników (można zmienić na powrót do formularza, jeśli wolisz)
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable("id") long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new EditProfileForm(user));
        }

        model.addAttribute("userId", id);
        model.addAttribute("userLogin", user.getLogin()); // Do wyświetlenia w nagłówku
        model.addAttribute("activeNav", "admin-users");

        return "admin/user-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable("id") long id,
                             @Valid @ModelAttribute("form") EditProfileForm form,
                             BindingResult bindingResult,
                             RedirectAttributes ra,
                             Model model) {

        if (bindingResult.hasErrors()) {
            User user = userRepository.findById(id).orElseThrow();
            model.addAttribute("userId", id);
            model.addAttribute("userLogin", user.getLogin());
            model.addAttribute("activeNav", "admin-users");
            return "admin/user-edit";
        }

        userAdminService.updateUser(id, form);
        ra.addFlashAttribute("successMessage", "Dane użytkownika zostały zaktualizowane.");

        return "redirect:/admin/users";
    }
}
