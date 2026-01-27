package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogReservationController {

    private final ReservationService reservationService;
    private final CurrentUserService currentUserService;

    public CatalogReservationController(ReservationService reservationService, CurrentUserService currentUserService) {
        this.reservationService = reservationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/catalog/{publicationId}/reserve")
    public String reserve(@PathVariable Long publicationId, RedirectAttributes ra) {
        var user = currentUserService.requireCurrentUser();

        try {
            reservationService.reservePublication(publicationId, user.getId());
            ra.addFlashAttribute("success", "Rezerwacja utworzona ✅");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/catalog/" + publicationId;
    }
}
