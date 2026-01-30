package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.*;
import com.example.neighborhood_library.repo.MessageRepository;
import com.example.neighborhood_library.repo.UserRepository;
import com.example.neighborhood_library.support.NotFoundException;
import com.example.neighborhood_library.web.dto.EditProfileForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private OperationService operationService;

    @InjectMocks
    private UserAdminService service;

    // --- Activate User ---

    @Test
    void activateUser_ShouldSetStatusToActive() {
        // given
        long userId = 10L;
        User user = new User();
        user.setStatus(UserStatus.INACTIVE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(currentUserService.requireCurrentUser()).thenReturn(new User());

        // when
        service.activateUser(userId);

        // then
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository).save(user);
        verify(operationService).logAction(any(), eq(user), eq("USER_ACTIVATED"), isNull());
    }

    // --- Ban User ---

    @Test
    void banUser_ShouldBanUser_WhenRulesAreMet() {
        // given
        long targetId = 20L;
        long adminId = 1L;

        User admin = new User();
        ReflectionTestUtils.setField(admin, "id", adminId);

        User target = new User();
        ReflectionTestUtils.setField(target, "id", targetId);
        target.setAccountRole(AccountRole.USER);
        target.setStatus(UserStatus.ACTIVE);

        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        // when
        service.banUser(targetId);

        // then
        assertEquals(UserStatus.BANNED, target.getStatus());
        verify(userRepository).save(target);

        // Weryfikacja wysłania wiadomości
        verify(messageRepository).save(any(Message.class));

        // Weryfikacja logu
        verify(operationService).logAction(admin, target, "USER_BANNED", null);
    }

    @Test
    void banUser_ShouldThrowException_WhenAdminTriesToBanSelf() {
        // given
        long adminId = 1L;
        User admin = new User();
        ReflectionTestUtils.setField(admin, "id", adminId);

        when(currentUserService.requireCurrentUser()).thenReturn(admin);

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.banUser(adminId) // Ten sam ID
        );
        assertEquals("Nie możesz zablokować własnego konta.", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    void banUser_ShouldThrowException_WhenTargetIsAlsoAdmin() {
        // given
        long targetId = 20L;
        long adminId = 1L;

        User admin = new User();
        ReflectionTestUtils.setField(admin, "id", adminId);

        User targetAdmin = new User();
        ReflectionTestUtils.setField(targetAdmin, "id", targetId);
        targetAdmin.setAccountRole(AccountRole.ADMIN); // TARGET IS ADMIN

        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(targetAdmin));

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.banUser(targetId)
        );
        assertEquals("Nie można zablokować innego Administratora.", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    // --- Unban User ---

    @Test
    void unbanUser_ShouldActivateUserAndSendMessage() {
        // given
        long targetId = 20L;
        User target = new User();
        target.setStatus(UserStatus.BANNED);

        when(currentUserService.requireCurrentUser()).thenReturn(new User());
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        // when
        service.unbanUser(targetId);

        // then
        assertEquals(UserStatus.ACTIVE, target.getStatus());
        verify(userRepository).save(target);
        verify(messageRepository).save(any(Message.class));
    }

    // --- Update User ---

    @Test
    void updateUser_ShouldUpdateProfileFields() {
        // given
        long targetId = 10L;
        User target = new User();
        target.setFirstName("OldName");

        EditProfileForm form = new EditProfileForm();
        form.setFirstName(" NewName "); // with whitespace to test trim
        form.setLastName("Doe");
        form.setPhone("123");
        form.setAddress("Street");

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(currentUserService.requireCurrentUser()).thenReturn(new User());

        // when
        service.updateUser(targetId, form);

        // then
        assertEquals("NewName", target.getFirstName());
        assertEquals("Doe", target.getLastName());
        verify(userRepository).save(target);
        verify(operationService).logAction(any(), eq(target), eq("USER_PROFILE_UPDATED_BY_ADMIN"), isNull());
    }
}