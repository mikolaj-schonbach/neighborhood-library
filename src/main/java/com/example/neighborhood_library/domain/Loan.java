package com.example.neighborhood_library.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK (reservation_id, copy_id, user_id) musi być spójne – db-plan :contentReference[oaicite:3]{index=3}
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private Copy copy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "loaned_at", nullable = false)
    private OffsetDateTime loanedAt;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @PrePersist
    void prePersist() {
        if (loanedAt == null) loanedAt = OffsetDateTime.now();
        if (dueDate == null) dueDate = LocalDate.now().plusDays(30);
    }

    // getters/setters
    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public Copy getCopy() { return copy; }
    public void setCopy(Copy copy) { this.copy = copy; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public OffsetDateTime getLoanedAt() { return loanedAt; }
    public void setLoanedAt(OffsetDateTime loanedAt) { this.loanedAt = loanedAt; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public OffsetDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(OffsetDateTime returnedAt) { this.returnedAt = returnedAt; }
}
