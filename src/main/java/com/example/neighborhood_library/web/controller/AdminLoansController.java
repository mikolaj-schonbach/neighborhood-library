package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.service.AdminLoanService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/loans")
public class AdminLoansController {

    private final AdminLoanService adminLoanService;

    public AdminLoansController(AdminLoanService adminLoanService) {
        this.adminLoanService = adminLoanService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("activeNav", "admin");
        model.addAttribute("loansPage", adminLoanService.activeLoans(PageRequest.of(page, size)));
        return "admin/loans";
    }

    @PostMapping("/{id}/return")
    public String acceptReturn(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminLoanService.acceptReturn(id);
            ra.addFlashAttribute("success", "Zwrot przyjęty ✅");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/loans";
    }
}
