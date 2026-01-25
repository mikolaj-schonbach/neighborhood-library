package com.example.neighborhood_library.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CITEXT w DB
    @Column(name = "first_name", nullable = false, columnDefinition = "citext")
    private String firstName;

    @Column(name = "last_name", nullable = false, columnDefinition = "citext")
    private String lastName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "author")
    private Set<PublicationAuthor> publicationAuthors = new LinkedHashSet<>();

    public Author() {}

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Instant getCreatedAt() { return createdAt; }

    public Set<PublicationAuthor> getPublicationAuthors() { return publicationAuthors; }
}
