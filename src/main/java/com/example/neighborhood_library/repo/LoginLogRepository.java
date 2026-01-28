package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}