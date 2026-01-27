package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadAtIsNullAndHiddenAtIsNull(Long userId);

}
