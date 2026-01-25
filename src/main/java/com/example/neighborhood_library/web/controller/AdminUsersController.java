package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.service.UserAdminService;
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
        return "admin/users";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable("id") long id) {
        userAdminService.activateUser(id);
        return "redirect:/admin/users?activated";
    }
}
