package com.example.neighborhood_library.repo;

import com.example.neighborhood_library.domain.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

    @Query(
            value = """
            select distinct p.id
            from Publication p
            left join p.publicationAuthors pa
            left join pa.author a
            left join p.copies c
            where c.deletedAt is null
              and (:categoryId is null or p.category.id = :categoryId)
              and (
                   :q is null or :q = ''
                   or lower(p.title) like lower(concat('%', :q, '%'))
                   or lower(a.firstName) like lower(concat('%', :q, '%'))
                   or lower(a.lastName) like lower(concat('%', :q, '%'))
              )
        """,
            countQuery = """
            select count(distinct p.id)
            from Publication p
            left join p.publicationAuthors pa
            left join pa.author a
            left join p.copies c
            where c.deletedAt is null
              and (:categoryId is null or p.category.id = :categoryId)
              and (
                   :q is null or :q = ''
                   or lower(p.title) like lower(concat('%', :q, '%'))
                   or lower(a.firstName) like lower(concat('%', :q, '%'))
                   or lower(a.lastName) like lower(concat('%', :q, '%'))
              )
        """
    )
    Page<Long> searchIds(String q, Long categoryId, Pageable pageable);

    @Query("""
        select distinct p
        from Publication p
        left join fetch p.category
        left join fetch p.publicationAuthors pa
        left join fetch pa.author
        left join fetch p.copies c
        where p.id in :ids
    """)
    List<Publication> findAllByIdInWithDetails(List<Long> ids);

    @Query("""
        select distinct p
        from Publication p
        left join fetch p.category
        left join fetch p.publicationAuthors pa
        left join fetch pa.author
        left join fetch p.copies c
        where p.id = :id
    """)
    Optional<Publication> findByIdWithDetails(long id);
}
