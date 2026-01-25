package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    // CITEXT => porównanie case-insensitive w DB
    Optional<Author> findByFirstNameAndLastName(String firstName, String lastName);
}
