package com.utang.repository;

import com.utang.domain.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    boolean existsByStoreIdAndDeviceId(Long storeId, String deviceId);
}
