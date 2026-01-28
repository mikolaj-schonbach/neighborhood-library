package com.example.neighborhood_library.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "library_info")
public class LibraryInfo {

    @Id
    private Short id = 1; // Zawsze 1

    @Column(nullable = false)
    private String address;

    @Column(name = "opening_hours", nullable = false)
    private String openingHours;

    @Column(nullable = false)
    private String rules;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LibraryInfo() {}

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    // getters & setters
    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; } // Raczej nie używane, bo zawsze 1

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public Instant getUpdatedAt() { return updatedAt; }
}