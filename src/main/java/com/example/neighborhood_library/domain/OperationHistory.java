package com.example.neighborhood_library.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "operations_history")
public class OperationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor; // Kto wykonał akcję (User lub Admin)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser; // Kogo dotyczy akcja (np. kogo zbanowano)

    @Column(nullable = false)
    private String action; // Enum jako String

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id")
    private Copy copy; // Jakiej kopii dotyczy (opcjonalne)

    @Column(name = "happened_at", nullable = false)
    private Instant happenedAt;

    @PrePersist
    void prePersist() {
        if (happenedAt == null) happenedAt = Instant.now();
    }

    public OperationHistory() {}

    public OperationHistory(User actor, User targetUser, String action, Copy copy) {
        this.actor = actor;
        this.targetUser = targetUser;
        this.action = action;
        this.copy = copy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public User getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(User targetUser) {
        this.targetUser = targetUser;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Copy getCopy() {
        return copy;
    }

    public void setCopy(Copy copy) {
        this.copy = copy;
    }

    public Instant getHappenedAt() {
        return happenedAt;
    }

    public void setHappenedAt(Instant happenedAt) {
        this.happenedAt = happenedAt;
    }
}
