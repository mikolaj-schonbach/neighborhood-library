package com.example.neighborhood_library.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "publications")
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationKind kind;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true)
    private String isbn;

    @Column(name = "year")
    private Short year;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PublicationAuthor> publicationAuthors = new LinkedHashSet<>();

    @OneToMany(mappedBy = "publication")
    private Set<Copy> copies = new LinkedHashSet<>();

    public Publication() {}

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void addAuthor(Author author) {
        PublicationAuthor link = new PublicationAuthor(this, author);
        publicationAuthors.add(link);
        author.getPublicationAuthors().add(link);
    }

    public Long getId() { return id; }

    public PublicationKind getKind() { return kind; }
    public void setKind(PublicationKind kind) { this.kind = kind; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Short getYear() { return year; }
    public void setYear(Short year) { this.year = year; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Set<PublicationAuthor> getPublicationAuthors() { return publicationAuthors; }
    public Set<Copy> getCopies() { return copies; }
}
