package com.utang.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * A device the store owner has verified (via OTP) and trusts. Logging in from a
 * trusted device skips the OTP step; a new device must be verified first.
 */
@Entity
@Table(name = "trusted_devices",
        uniqueConstraints = @UniqueConstraint(name = "uq_trusted_device",
                columnNames = {"store_id", "device_id"}))
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected TrustedDevice() {
    }

    public TrustedDevice(Long storeId, String deviceId) {
        this.storeId = storeId;
        this.deviceId = deviceId;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
