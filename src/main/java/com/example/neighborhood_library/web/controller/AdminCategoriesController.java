package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.service.AdminCategoryService;
import com.example.neighborhood_library.web.dto.CategoryForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoriesController {

    private final CategoryRepository categoryRepository;
    private final AdminCategoryService adminCategoryService;

    public AdminCategoriesController(CategoryRepository categoryRepository, AdminCategoryService adminCategoryService) {
        this.categoryRepository = categoryRepository;
        this.adminCategoryService = adminCategoryService;
    }

    @GetMapping
    public String page(Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new CategoryForm());
        }
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        return "admin/categories";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            return "admin/categories";
        }

        try {
            adminCategoryService.create(form.getName());
            ra.addFlashAttribute("successMessage", "Dodano kategorię.");
            return "redirect:/admin/categories";
        } catch (Exception e) {
            model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            model.addAttribute("errorMessage", "Nie udało się dodać kategorii (możliwy duplikat).");
            return "admin/categories";
        }
    }

    @PostMapping("/{id}/rename")
    public String rename(@PathVariable("id") long id,
                         @RequestParam("name") String name,
                         RedirectAttributes ra) {
        try {
            adminCategoryService.rename(id, name);
            ra.addFlashAttribute("successMessage", "Zmieniono nazwę kategorii.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Nie udało się zmienić nazwy (możliwy duplikat).");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") long id, RedirectAttributes ra) {
        try {
            adminCategoryService.delete(id);
            ra.addFlashAttribute("successMessage", "Usunięto kategorię.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Nie udało się usunąć kategorii.");
        }
        return "redirect:/admin/categories";
    }
}
