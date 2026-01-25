package com.example.neighborhood_library.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PublicationAuthorId implements Serializable {

    private Long publicationId;
    private Long authorId;

    public PublicationAuthorId() {}

    public PublicationAuthorId(Long publicationId, Long authorId) {
        this.publicationId = publicationId;
        this.authorId = authorId;
    }

    public Long getPublicationId() { return publicationId; }
    public Long getAuthorId() { return authorId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicationAuthorId that)) return false;
        return Objects.equals(publicationId, that.publicationId) &&
                Objects.equals(authorId, that.authorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicationId, authorId);
    }
}
