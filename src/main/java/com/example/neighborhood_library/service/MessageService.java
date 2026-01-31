package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Message;
import com.example.neighborhood_library.repo.MessageRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final Clock clock;

    public MessageService(MessageRepository messageRepository, Clock clock) {
        this.messageRepository = messageRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Message> getMyMessages(Long userId) {
        return messageRepository.findByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return messageRepository.countByUserIdAndReadAtIsNullAndHiddenAtIsNull(userId);
    }

    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message msg = getMessageOrThrow(messageId, userId);
        if (msg.getReadAt() == null) {
            msg.setReadAt(OffsetDateTime.now(clock));
            messageRepository.save(msg);
        }
    }

    @Transactional
    public void delete(Long messageId, Long userId) {
        Message msg = getMessageOrThrow(messageId, userId);

        // Zgodnie z bazą danych (CHECK constraint): hidden_at IS NULL OR read_at IS NOT NULL
        // Zatem przed ukryciem musi być oznaczona jako przeczytana.
        if (msg.getReadAt() == null) {
            msg.setReadAt(OffsetDateTime.now(clock));
        }

        msg.setHiddenAt(OffsetDateTime.now(clock));
        messageRepository.save(msg);
    }

    private Message getMessageOrThrow(Long messageId, Long userId) {
        return messageRepository.findById(messageId)
                .filter(m -> m.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Nie znaleziono wiadomości lub brak dostępu."));
    }
}
