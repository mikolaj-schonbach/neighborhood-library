package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.AuthService;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.web.dto.ChangePasswordForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/my/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final AuthService authService;

    public ProfileController(CurrentUserService currentUserService, AuthService authService) {
        this.currentUserService = currentUserService;
        this.authService = authService;
    }

    @GetMapping
    public String showProfile(Model model) {
        User user = currentUserService.requireCurrentUser();
        model.addAttribute("user", user);

        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
        }
        model.addAttribute("activeNav", "profile"); // Opcjonalnie dodać do layoutu

        return "my/profile";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes ra,
                                 Model model) {

        User user = currentUserService.requireCurrentUser();

        if (!form.getNewPassword().equals(form.getConfirmNewPassword())) {
            bindingResult.addError(new FieldError("passwordForm", "confirmNewPassword", "Hasła nie są takie same."));
        }

        if (bindingResult.hasErrors()) {
            // Przy błędzie musimy ponownie załadować dane użytkownika do widoku
            model.addAttribute("user", user);
            model.addAttribute("activeNav", "profile");
            return "my/profile";
        }

        try {
            authService.changePassword(user.getId(), form.getOldPassword(), form.getNewPassword());
            ra.addFlashAttribute("success", "Hasło zostało zmienione.");
            return "redirect:/my/profile";
        } catch (IllegalArgumentException e) {
            bindingResult.addError(new FieldError("passwordForm", "oldPassword", e.getMessage()));
            model.addAttribute("user", user);
            model.addAttribute("activeNav", "profile");
            return "my/profile";
        }
    }
}
