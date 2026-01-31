package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.LibraryInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryInfoRepository extends JpaRepository<LibraryInfo, Long> {
}
