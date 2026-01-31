package com.example.neighborhood_library.web.controller;

import com.example.neighborhood_library.config.SecurityConfig;
import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.ReservationStatus;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.domain.UserStatus;
import com.example.neighborhood_library.repo.LoanRepository;
import com.example.neighborhood_library.repo.ReservationRepository;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.service.CurrentUserService;
import com.example.neighborhood_library.service.LibraryInfoService;
import com.example.neighborhood_library.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDashboardController.class)
@Import(SecurityConfig.class)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean private ReservationRepository reservationRepository;
    @MockitoBean private LoanRepository loanRepository;

    // --- Mocki dla GlobalControllerAdvice ---
    @MockitoBean private LibraryInfoService libraryInfoService;
    @MockitoBean private MessageService messageService;
    @MockitoBean private CurrentUserService currentUserService;
    // ---------------------------------------

    @BeforeEach
    void setUp() {
        User adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        adminUser.setLogin("admin");
        adminUser.setAccountRole(AccountRole.ADMIN);

        when(currentUserService.requireCurrentUser()).thenReturn(adminUser);
    }

    // --- SECURITY CHECKS ---

    @Test
    void dashboard_ShouldRedirectToLogin_WhenAnonymous() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void dashboard_ShouldForbidAccess_ForRegularUser() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboard_ShouldReturnView_AndPopulateCounters_ForAdmin() throws Exception {
        // given
        LocalDate today = LocalDate.now();

        when(userRepository.countByStatus(UserStatus.INACTIVE)).thenReturn(3L);
        when(reservationRepository.countByStatus(ReservationStatus.ACTIVE)).thenReturn(7L);
        when(loanRepository.countByDueDateBeforeAndReturnedAtIsNull(today)).thenReturn(2L);

        // when + then
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("activeNav", "admin-dashboard"))
                .andExpect(model().attribute("inactiveUsersCount", 3L))
                .andExpect(model().attribute("activeReservationsCount", 7L))
                .andExpect(model().attribute("overdueLoansCount", 2L));

        verify(userRepository).countByStatus(UserStatus.INACTIVE);
        verify(reservationRepository).countByStatus(ReservationStatus.ACTIVE);
        verify(loanRepository).countByDueDateBeforeAndReturnedAtIsNull(today);
        verifyNoMoreInteractions(userRepository, reservationRepository, loanRepository);
    }
}
