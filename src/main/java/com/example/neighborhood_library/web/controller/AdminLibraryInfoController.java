package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.service.LibraryInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/info")
public class AdminLibraryInfoController {

    private final LibraryInfoService libraryInfoService;

    public AdminLibraryInfoController(LibraryInfoService libraryInfoService) {
        this.libraryInfoService = libraryInfoService;
    }

    @GetMapping
    public String editForm(Model model) {
        model.addAttribute("activeNav", "admin-info");
        // libraryInfo jest już w modelu dzięki GlobalControllerAdvice,
        // ale dla jasności kontrolera można to pominąć lub zostawić.
        return "admin/library-info";
    }

    @PostMapping
    public String update(@RequestParam String address,
                         @RequestParam String openingHours,
                         @RequestParam String rules,
                         RedirectAttributes ra) {
        libraryInfoService.updateInfo(address, openingHours, rules);
        ra.addFlashAttribute("successMessage", "Zaktualizowano informacje o bibliotece.");
        return "redirect:/admin/info";
    }
}
