package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.ReservationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MyBooksController {

    private final CurrentUserService currentUserService;
    private final ReservationService reservationService;
    private final LoanRepository loanRepository;

    public MyBooksController(CurrentUserService currentUserService,
                             ReservationService reservationService,
                             LoanRepository loanRepository) {
        this.currentUserService = currentUserService;
        this.reservationService = reservationService;
        this.loanRepository = loanRepository;
    }

    @GetMapping("/my/books")
    public String myBooks(Model model) {
        var user = currentUserService.requireCurrentUser();
        model.addAttribute("activeNav", "my-books");
        model.addAttribute("reservations", reservationService.myActiveReservations(user.getId()));
        model.addAttribute("loans", loanRepository.findByUserIdAndReturnedAtIsNullOrderByLoanedAtDesc(user.getId()));
        return "my/books";
    }

    @PostMapping("/my/reservations/{reservationId}/cancel")
    public String cancel(@PathVariable Long reservationId, RedirectAttributes ra) {
        var user = currentUserService.requireCurrentUser();
        try {
            reservationService.cancelByUser(reservationId, user.getId());
            ra.addFlashAttribute("success", "Rezerwacja anulowana ✅");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/my/books";
    }
}
