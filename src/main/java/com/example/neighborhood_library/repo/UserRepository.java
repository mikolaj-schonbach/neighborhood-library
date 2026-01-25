package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);
    List<User> findByStatus(UserStatus status);
}
