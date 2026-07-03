package com.utang.repository;

import com.utang.domain.Store;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByPhoneNumber(String phoneNumber);

    Optional<Store> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Reads only the QR image bytes for a store. Kept separate so the (potentially
     * large) blob is never loaded when resolving the store for ordinary requests.
     */
    @Query(value = "SELECT qr_code_image FROM stores WHERE id = :id", nativeQuery = true)
    byte[] findQrCodeImageById(@Param("id") Long id);

    /** Writes (or clears) the QR image bytes and content type without touching other columns. */
    @Modifying
    @Query(value = "UPDATE stores SET qr_code_image = :image, qr_code_content_type = :contentType "
            + "WHERE id = :id", nativeQuery = true)
    void updateQrCode(@Param("id") Long id,
                      @Param("image") byte[] image,
                      @Param("contentType") String contentType);
}
