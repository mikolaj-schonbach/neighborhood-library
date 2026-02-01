package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.ReservationService;
import com.example.neighborhood_library.support.ReservationBlockedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/catalog")
public class CatalogReservationController {

    private final ReservationService reservationService;
    private final CurrentUserService currentUserService;

    public CatalogReservationController(ReservationService reservationService, CurrentUserService currentUserService) {
        this.reservationService = reservationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{publicationId}/reserve")
    public String reserve(@PathVariable Long publicationId, RedirectAttributes redirectAttributes) {
        User currentUser = currentUserService.requireCurrentUser();
        try {
            var reservation = reservationService.reservePublication(publicationId, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Dokonano rezerwacji publikacji: " +
                reservation.getCopy().getPublication().getTitle());
        } catch (IllegalStateException | ReservationBlockedException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/catalog/" + publicationId;
    }
}
