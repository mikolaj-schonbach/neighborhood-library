package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    @Query("""
        select distinct c
        from Category c
        join c.publications p
        join p.copies cp
        where cp.deletedAt is null
        order by c.name
    """)
    List<Category> findNonEmptyForCatalog();
}
