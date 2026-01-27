package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
