package com.example.neighborhood_library.web.dto;

import com.example.neighborhood_library.domain.PublicationKind;
import jakarta.validation.constraints.*;

public class AddPublicationForm {

    @NotBlank(message = "Tytuł jest wymagany.")
    private String title;

    @NotBlank(message = "Autorzy są wymagani (np. 'Jan Kowalski; Anna Nowak').")
    private String authors; // format: "Imię Nazwisko; Imię Nazwisko"

    @NotNull(message = "Rodzaj jest wymagany.")
    private PublicationKind kind;

    @NotNull(message = "Kategoria jest wymagana.")
    private Long categoryId;

    @Size(max = 50, message = "ISBN jest za długi.")
    private String isbn;

    @Min(value = 1400, message = "Rok musi być >= 1400.")
    @Max(value = 2100, message = "Rok musi być <= 2100.")
    private Short year;

    // getters/setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public PublicationKind getKind() { return kind; }
    public void setKind(PublicationKind kind) { this.kind = kind; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public Short getYear() { return year; }
    public void setYear(Short year) { this.year = year; }
}
