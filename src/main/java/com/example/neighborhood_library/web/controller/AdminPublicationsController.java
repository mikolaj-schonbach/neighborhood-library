package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.Publication;
import com.example.neighborhood_library.domain.PublicationAuthor;
import com.example.neighborhood_library.domain.PublicationKind;
import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.repo.PublicationRepository;
import com.example.neighborhood_library.service.AdminPublicationService;
import com.example.neighborhood_library.support.NotFoundException;
import com.example.neighborhood_library.web.dto.AddPublicationForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/publications")
public class AdminPublicationsController {

    private final CategoryRepository categoryRepository;
    private final PublicationRepository publicationRepository;
    private final AdminPublicationService adminPublicationService;

    public AdminPublicationsController(CategoryRepository categoryRepository, PublicationRepository publicationRepository,
                                       AdminPublicationService adminPublicationService) {
        this.categoryRepository = categoryRepository;
        this.publicationRepository = publicationRepository;
        this.adminPublicationService = adminPublicationService;
    }

    @GetMapping("/new")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new AddPublicationForm());
        }
        model.addAttribute("kinds", PublicationKind.values());
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("activeNav", "admin-publications");

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

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        Publication p = publicationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Publikacja nie istnieje"));

        if (!model.containsAttribute("form")) {
            AddPublicationForm form = new AddPublicationForm();
            form.setTitle(p.getTitle());
            form.setKind(p.getKind());
            form.setCategoryId(p.getCategory().getId());
            form.setIsbn(p.getIsbn());
            form.setYear(p.getYear());

            // Konwersja autorów do stringa
            String authorsStr = p.getPublicationAuthors().stream()
                    .map(PublicationAuthor::getAuthor)
                    .map(a -> a.getFirstName() + " " + a.getLastName())
                    .collect(Collectors.joining("; "));
            form.setAuthors(authorsStr);

            model.addAttribute("form", form);
        }

        model.addAttribute("publication", p); // Do wyświetlenia listy kopii
        model.addAttribute("kinds", PublicationKind.values());
        model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("activeNav", "admin-publications");

        return "admin/publication-edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("form") AddPublicationForm form,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model) {

        if (bindingResult.hasErrors()) {
            // Przeładuj dane potrzebne do widoku
            Publication p = publicationRepository.findByIdWithDetails(id).orElseThrow();
            model.addAttribute("publication", p);
            model.addAttribute("kinds", PublicationKind.values());
            model.addAttribute("categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            return "admin/publication-edit";
        }

        try {
            adminPublicationService.editPublication(id, form.getTitle(), form.getAuthors(), form.getKind(),
                    form.getCategoryId(), form.getIsbn(), form.getYear());
            ra.addFlashAttribute("successMessage", "Zapisano zmiany.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/publications/" + id + "/edit";
    }

    @PostMapping("/{id}/copies/add")
    public String addCopy(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            adminPublicationService.addCopy(id);
            ra.addFlashAttribute("successMessage", "Dodano nowy egzemplarz.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Błąd: " + e.getMessage());
        }
        return "redirect:/admin/publications/" + id + "/edit";
    }

    @PostMapping("/{id}/copies/{copyId}/delete")
    public String deleteCopy(@PathVariable("id") Long id, @PathVariable("copyId") Long copyId, RedirectAttributes ra) {
        try {
            adminPublicationService.deleteCopy(copyId);
            ra.addFlashAttribute("successMessage", "Egzemplarz usunięty (soft delete).");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Nie można usunąć: " + e.getMessage());
        }
        return "redirect:/admin/publications/" + id + "/edit";
    }
}
