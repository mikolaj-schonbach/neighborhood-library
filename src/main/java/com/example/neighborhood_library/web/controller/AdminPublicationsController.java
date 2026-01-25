package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.PublicationKind;
import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.service.AdminPublicationService;
import com.example.neighborhood_library.web.dto.AddPublicationForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/publications")
public class AdminPublicationsController {

    private final CategoryRepository categoryRepository;
    private final AdminPublicationService adminPublicationService;

    public AdminPublicationsController(CategoryRepository categoryRepository,
                                       AdminPublicationService adminPublicationService) {
        this.categoryRepository = categoryRepository;
        this.adminPublicationService = adminPublicationService;
    }

    @GetMapping("/new")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new AddPublicationForm());
        }
        model.addAttribute("kinds", PublicationKind.values());
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        return "admin/publication-new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AddPublicationForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("kinds", PublicationKind.values());
            model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            return "admin/publication-new";
        }

        try {
            long pubId = adminPublicationService.createPublicationWithOneCopy(
                    form.getTitle(),
                    form.getAuthors(),
                    form.getKind(),
                    form.getCategoryId(),
                    form.getIsbn(),
                    form.getYear()
            );

            ra.addFlashAttribute("successMessage", "Dodano publikację i 1 egzemplarz.");
            return "redirect:/catalog/" + pubId;
        } catch (IllegalArgumentException e) {
            model.addAttribute("kinds", PublicationKind.values());
            model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/publication-new";
        } catch (Exception e) {
            model.addAttribute("kinds", PublicationKind.values());
            model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            model.addAttribute("errorMessage", "Nie udało się dodać publikacji (sprawdź ISBN / duplikaty).");
            return "admin/publication-new";
        }
    }
}
