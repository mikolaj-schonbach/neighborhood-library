package com.example.neighborhood_library.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private Copy copy;

    @Column(name = "reserved_at", nullable = false)
    private OffsetDateTime reservedAt;

    @Column(name = "pickup_until", nullable = false)
    private OffsetDateTime pickupUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "expired_at")
    private OffsetDateTime expiredAt;

    @Column(name = "fulfilled_at")
    private OffsetDateTime fulfilledAt;

    @PrePersist
    void prePersist() {
        if (reservedAt == null) reservedAt = OffsetDateTime.now();
        if (status == null) status = ReservationStatus.ACTIVE;
    }

    // getters/setters

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Copy getCopy() { return copy; }
    public void setCopy(Copy copy) { this.copy = copy; }
    public OffsetDateTime getReservedAt() { return reservedAt; }
    public void setReservedAt(OffsetDateTime reservedAt) { this.reservedAt = reservedAt; }
    public OffsetDateTime getPickupUntil() { return pickupUntil; }
    public void setPickupUntil(OffsetDateTime pickupUntil) { this.pickupUntil = pickupUntil; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public OffsetDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(OffsetDateTime expiredAt) { this.expiredAt = expiredAt; }
    public OffsetDateTime getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(OffsetDateTime fulfilledAt) { this.fulfilledAt = fulfilledAt; }
}
