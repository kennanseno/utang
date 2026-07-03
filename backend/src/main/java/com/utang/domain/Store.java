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

    @Column(unique = true)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String name = "My Store";

    @Column(name = "owner_name")
    private String ownerName;

    /** True once the owner has completed onboarding (set their store profile). */
    @Column(nullable = false)
    private boolean onboarded = false;

    /** True once the owner has verified ownership of their mobile number via OTP. */
    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    /**
     * MIME type of the stored QR code image (e.g. image/png), or null when none is
     * uploaded. The image bytes themselves live in the {@code qr_code_image} column
     * but are intentionally NOT mapped here, so they aren't loaded on every request;
     * they are read/written via dedicated repository queries. This column doubles as
     * a cheap "has QR code?" flag.
     */
    @Column(name = "qr_code_content_type")
    private String qrCodeContentType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Store() {
    }

    public Store(String phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.name = name;
    }

    /** Registration constructor: full store owner account with credentials. */
    public Store(String username, String passwordHash, String phoneNumber, String name, String ownerName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.ownerName = ownerName;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public String getQrCodeContentType() {
        return qrCodeContentType;
    }

    public void setQrCodeContentType(String qrCodeContentType) {
        this.qrCodeContentType = qrCodeContentType;
    }

    public boolean hasQrCode() {
        return qrCodeContentType != null && !qrCodeContentType.isBlank();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
