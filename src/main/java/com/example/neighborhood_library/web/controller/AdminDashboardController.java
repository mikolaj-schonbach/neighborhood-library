package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.domain.ReservationStatus;
import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import com.example.neighborhood_library.repo.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final LoanRepository loanRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    ReservationRepository reservationRepository,
                                    LoanRepository loanRepository) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.loanRepository = loanRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("activeNav", "admin-dashboard");

        // Liczniki do kafelków
        model.addAttribute("inactiveUsersCount", userRepository.countByStatus(UserStatus.INACTIVE));
        model.addAttribute("activeReservationsCount", reservationRepository.countByStatus(ReservationStatus.ACTIVE));
        model.addAttribute("overdueLoansCount",
            loanRepository.countByDueDateBeforeAndReturnedAtIsNull(LocalDate.now()));

        return "admin/dashboard";
    }
}
