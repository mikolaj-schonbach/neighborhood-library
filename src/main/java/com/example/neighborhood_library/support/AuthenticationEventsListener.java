package com.example.neighborhood_library.support;

import com.example.neighborhood_library.config.LibraryUserPrincipal;
import com.example.neighborhood_library.domain.LoginLog;
import com.example.neighborhood_library.repo.LoginLogRepository;
import com.example.neighborhood_library.repo.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthenticationEventsListener {

    private final LoginLogRepository loginLogRepository;
    private final UserRepository userRepository;

    public AuthenticationEventsListener(LoginLogRepository loginLogRepository, UserRepository userRepository) {
        this.loginLogRepository = loginLogRepository;
        this.userRepository = userRepository;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Nowa transakcja, żeby log zapisał się nawet przy błędzie
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof LibraryUserPrincipal principal) {
            userRepository.findById(principal.getId()).ifPresent(user -> {
                LoginLog log = new LoginLog(user.getLogin(), true, user);
                loginLogRepository.save(log);
            });
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String login = (String) event.getAuthentication().getPrincipal();
        // Próbujemy znaleźć użytkownika po loginie (nawet jeśli hasło błędne)
        var user = userRepository.findByLogin(login).orElse(null);

        LoginLog log = new LoginLog(login, false, user);
        loginLogRepository.save(log);
    }
}