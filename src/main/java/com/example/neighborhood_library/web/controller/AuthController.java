package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.service.AuthService;
import com.example.neighborhood_library.support.DuplicateLoginException;
import com.example.neighborhood_library.web.dto.RegisterForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerForm") RegisterForm form,
                                 BindingResult bindingResult,
                                 Model model) {

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.addError(new FieldError("registerForm", "confirmPassword",
                    "Hasła nie są takie same."));
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(
                    form.getFirstName(),
                    form.getLastName(),
                    form.getLogin(),
                    form.getPassword(),
                    form.getPhone(),
                    form.getAddress()
            );
        } catch (DuplicateLoginException e) {
            bindingResult.addError(new FieldError("registerForm", "login", e.getMessage()));
            return "auth/register";
        }

        // komunikat po rejestracji
        return "redirect:/register?success";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}
