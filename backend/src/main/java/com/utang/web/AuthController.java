package com.utang.web;

import com.utang.domain.Store;
import com.utang.dto.Dtos.AuthResponse;
import com.utang.dto.Dtos.EmailVerificationRequest;
import com.utang.dto.Dtos.EmailVerificationResponse;
import com.utang.dto.Dtos.LoginRequest;
import com.utang.dto.Dtos.RegisterRequest;
import com.utang.dto.Dtos.StoreResponse;
import com.utang.dto.Dtos.UpdateStoreRequest;
import com.utang.security.CurrentStore;
import com.utang.security.TokenService;
import com.utang.service.AuthService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    /** Registers a new store owner and signs them in. */
    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        Store store = authService.register(
                request.username(), request.password(), request.phoneNumber(),
                request.email(), request.storeName(), request.ownerName());
        return new AuthResponse(tokenService.issue(store.getId()), toResponse(store));
    }

    /** Logs in with username + password. */
    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Store store = authService.login(request.username(), request.password());
        return new AuthResponse(tokenService.issue(store.getId()), toResponse(store));
    }

    @GetMapping("/me")
    public StoreResponse me(@CurrentStore Store store) {
        return toResponse(store);
    }

    /** Updates the store profile for the authenticated owner. */
    @PutMapping("/store")
    public StoreResponse updateStore(@CurrentStore Store store, @Valid @RequestBody UpdateStoreRequest request) {
        Store updated = authService.updateStore(
                store, request.storeName(), request.ownerName(), request.phoneNumber(), request.email());
        return toResponse(updated);
    }

    /** Sends a code to the owner's email so they can verify they own the address. */
    @PostMapping("/store/email/verify/request")
    public EmailVerificationResponse requestEmailVerification(@CurrentStore Store store) {
        String code = authService.requestEmailVerification(store);
        String devCode = authService.isLiveEmail() ? null : code;
        return new EmailVerificationResponse(maskEmail(store.getEmail()), devCode);
    }

    /** Confirms the code and marks the owner's email address as verified. */
    @PostMapping("/store/email/verify/confirm")
    public StoreResponse confirmEmailVerification(
            @CurrentStore Store store,
            @Valid @RequestBody EmailVerificationRequest request) {
        Store verified = authService.confirmEmailVerification(store, request.code());
        return toResponse(verified);
    }

    /** Uploads (or replaces) the owner's payment QR code image. */
    @PutMapping("/store/qr")
    public StoreResponse uploadQrCode(@CurrentStore Store store,
                                      @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("QR code image is required");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file");
        }
        Store updated = authService.updateQrCode(store, bytes, file.getContentType());
        return toResponse(updated);
    }

    /** Returns the owner's payment QR code image, or 404 if none has been uploaded. */
    @GetMapping("/store/qr")
    public ResponseEntity<byte[]> getQrCode(@CurrentStore Store store) {
        if (!store.hasQrCode()) {
            return ResponseEntity.notFound().build();
        }
        byte[] image = authService.loadQrCodeImage(store);
        if (image == null || image.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(store.getQrCodeContentType()))
                .body(image);
    }

    /** Removes the owner's payment QR code image. */
    @DeleteMapping("/store/qr")
    public StoreResponse removeQrCode(@CurrentStore Store store) {
        return toResponse(authService.removeQrCode(store));
    }

    /** Masks an email, revealing only the first character and domain (e.g. n•••@gmail.com). */
    private static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "•••" + email.substring(at);
    }

    private static StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getUsername(),
                store.getPhoneNumber(),
                store.getEmail(),
                store.getName(),
                store.getOwnerName(),
                store.isOnboarded(),
                store.isEmailVerified(),
                store.hasQrCode());
    }
}
