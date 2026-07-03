package com.utang.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String name = "My Store";

    @Column(name = "owner_name")
    private String ownerName;

    /** True once the owner has completed onboarding (set their store profile). */
    @Column(nullable = false)
    private boolean onboarded = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Store() {
    }

    public Store(String phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isOnboarded() {
        return onboarded;
    }

    public void markOnboarded() {
        this.onboarded = true;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
