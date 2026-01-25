package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Copy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CopyRepository extends JpaRepository<Copy, Long> {
}
