package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.MessageRepository;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final CurrentUserService currentUserService;
    private final OperationService operationService;

    public UserAdminService(UserRepository userRepository, MessageRepository messageRepository, CurrentUserService currentUserService, OperationService operationService) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.currentUserService = currentUserService;
        this.operationService = operationService;
    }

    @Transactional
    public void activateUser(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika id=" + userId));

        user.setStatus(UserStatus.ACTIVE);

        User currentUser = currentUserService.requireCurrentUser();
        // save niekonieczny przy JPA w transakcji, ale może zostać:
        userRepository.save(user);

        operationService.logAction(currentUser, user, "USER_ACTIVATED", null);
    }

    @Transactional
    public void banUser(long userId) {
        User currentUser = currentUserService.requireCurrentUser();

        // Blokada "samobójstwa"
        if (Objects.equals(currentUser.getId(), userId)) {
            throw new IllegalArgumentException("Nie możesz zablokować własnego konta.");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika id=" + userId));

        // 2. Blokada "wojny adminów" - nie można zbanować innego admina
        if (targetUser.getAccountRole() == AccountRole.ADMIN) {
            throw new IllegalArgumentException("Nie można zablokować innego Administratora.");
        }

        targetUser.setStatus(UserStatus.BANNED);
        userRepository.save(targetUser);


        // US-016: Zablokowany użytkownik otrzymuje powiadomienie systemowe
        sendMessage(targetUser, MessageType.ACCOUNT_BANNED, "Konto zablokowane",
                "Twoje konto zostało zablokowane. Skontaktuj się z biblioteką w celu wyjaśnienia.");

        operationService.logAction(currentUser, targetUser, "USER_BANNED", null);
    }

    @Transactional
    public void unbanUser(long userId) {

        User currentUser = currentUserService.requireCurrentUser();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono użytkownika id=" + userId));

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        sendMessage(user, MessageType.ACCOUNT_UNBANNED, "Konto odblokowane",
                "Blokada Twojego konta została zdjęta.");

        operationService.logAction(currentUser, user, "USER_BANNED", null);
    }

    private void sendMessage(User user, MessageType type, String title, String body) {
        Message msg = new Message();
        msg.setUser(user);
        msg.setType(type);
        msg.setTitle(title);
        msg.setBody(body);
        messageRepository.save(msg);
    }
}
