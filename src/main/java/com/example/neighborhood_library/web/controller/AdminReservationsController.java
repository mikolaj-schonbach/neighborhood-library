package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.service.AdminReservationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reservations")
public class AdminReservationsController {

    private final AdminReservationService adminReservationService;

    public AdminReservationsController(AdminReservationService adminReservationService) {
        this.adminReservationService = adminReservationService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("activeNav", "admin");
        model.addAttribute("reservationsPage", adminReservationService.activeReservations(PageRequest.of(page, size)));
        return "admin/reservations";
    }

    @PostMapping("/{id}/issue")
    public String issue(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminReservationService.issueLoan(id);
            ra.addFlashAttribute("success", "Wypożyczenie utworzone ✅");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/reservations";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminReservationService.cancelByAdmin(id);
            ra.addFlashAttribute("success", "Rezerwacja anulowana ✅");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/reservations";
    }
}
