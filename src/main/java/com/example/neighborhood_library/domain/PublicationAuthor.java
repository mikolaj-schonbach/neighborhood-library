package com.example.neighborhood_library.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "publications_authors")
public class PublicationAuthor {

    @EmbeddedId
    private PublicationAuthorId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("publicationId")
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("authorId")
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    public PublicationAuthor() {}

    public PublicationAuthor(Publication publication, Author author) {
        this.publication = publication;
        this.author = author;
        this.id = new PublicationAuthorId(publication.getId(), author.getId());
    }

    @PostLoad
    void postLoadFixIdIfNeeded() {
        if (id == null && publication != null && author != null) {
            id = new PublicationAuthorId(publication.getId(), author.getId());
        }
    }

    public PublicationAuthorId getId() { return id; }
    public Publication getPublication() { return publication; }
    public Author getAuthor() { return author; }
}
