package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Message;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.repo.MessageRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;

    private final Instant fixedInstant = Instant.parse("2024-01-01T12:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

    private MessageService service;

    @BeforeEach
    void setUp() {
        service = new MessageService(messageRepository, fixedClock);
    }

    @Test
    void markAsRead_ShouldSetReadAt_WhenNotReadYet() {
        // given
        Long msgId = 1L;
        Long userId = 100L;

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        Message msg = new Message();
        msg.setUser(user);
        msg.setReadAt(null); // nieprzeczytana

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

        // when
        service.markAsRead(msgId, userId);

        // then
        assertNotNull(msg.getReadAt());
        verify(messageRepository).save(msg);
    }

    @Test
    void delete_ShouldMarkAsReadAndHidden_WhenDeletingUnreadMessage() {
        // given
        Long msgId = 1L;
        Long userId = 100L;

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        Message msg = new Message();
        msg.setUser(user);
        msg.setReadAt(null);
        msg.setHiddenAt(null);

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

        // when
        service.delete(msgId, userId);

        // then
        assertNotNull(msg.getReadAt(), "Musi oznaczyć jako przeczytane przed ukryciem");
        assertNotNull(msg.getHiddenAt(), "Musi ustawić flagę ukrycia");
        verify(messageRepository).save(msg);
    }

    @Test
    void delete_ShouldThrowNotFound_WhenMessageBelongsToOtherUser() {
        // given
        Long msgId = 1L;
        Long userId = 100L;
        Long otherUserId = 999L;

        User otherUser = new User();
        ReflectionTestUtils.setField(otherUser, "id", otherUserId);

        Message msg = new Message();
        msg.setUser(otherUser);

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

        // when & then
        assertThrows(NotFoundException.class, () -> service.delete(msgId, userId));
    }
}
