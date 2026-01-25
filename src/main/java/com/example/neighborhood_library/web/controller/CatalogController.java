package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.service.CatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final CategoryRepository categoryRepository;

    public CatalogController(CatalogService catalogService, CategoryRepository categoryRepository) {
        this.catalogService = catalogService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String index(@RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "categoryId", required = false) Long categoryId,
                        @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                        Model model) {

        int pageSize = 10;

        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", categoryRepository.findNonEmptyForCatalog());
        model.addAttribute("page", catalogService.search(q, categoryId, page, pageSize));

        return "catalog/index";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable("id") long id, Model model) {
        model.addAttribute("publication", catalogService.getDetails(id));
        return "catalog/details";
    }
}
