package com.example.neighborhood_library.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_logs")
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Może być null (gdy logowanie błędnym loginem)
    private User user;

    @Column(columnDefinition = "citext")
    private String login; // Snapshot wpisanego loginu

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @Column(nullable = false)
    private boolean success;

    @PrePersist
    void prePersist() {
        if (loggedAt == null) loggedAt = Instant.now();
    }

    // Konstruktory, gettery, settery
    public LoginLog() {}

    public LoginLog(String login, boolean success, User user) {
        this.login = login;
        this.success = success;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Instant loggedAt) {
        this.loggedAt = loggedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
