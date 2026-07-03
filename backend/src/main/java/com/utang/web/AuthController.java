package com.utang.web;

import com.utang.domain.Store;
import com.utang.dto.Dtos.AuthResponse;
import com.utang.dto.Dtos.LoginRequest;
import com.utang.dto.Dtos.LoginResponse;
import com.utang.dto.Dtos.PhoneVerificationRequest;
import com.utang.dto.Dtos.PhoneVerificationResponse;
import com.utang.dto.Dtos.RegisterRequest;
import com.utang.dto.Dtos.StoreResponse;
import com.utang.dto.Dtos.UpdateStoreRequest;
import com.utang.dto.Dtos.VerifyDeviceRequest;
import com.utang.security.CurrentStore;
import com.utang.security.TokenService;
import com.utang.service.AuthService;
import com.utang.service.AuthService.LoginResult;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AuthController {

    private static final String DEVICE_HEADER = "X-Device-Id";

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    /** Registers a new store owner and signs them in on the current device. */
    @PostMapping("/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId) {
        Store store = authService.register(
                request.username(), request.password(), request.phoneNumber(),
                request.storeName(), request.ownerName(), deviceId);
        return new AuthResponse(tokenService.issue(store.getId()), toResponse(store));
    }

    /**
     * Logs in with username + password. On a trusted device the response is
     * {@code AUTHENTICATED}; from a new device it is {@code OTP_REQUIRED} and an
     * OTP is sent to the owner's mobile number (complete via {@code /auth/verify-device}).
     */
    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId) {
        LoginResult result = authService.login(request.username(), request.password(), deviceId);
        if (result.otpRequired()) {
            String devCode = authService.isLiveSms() ? null : result.devCode();
            return LoginResponse.otpRequired(maskPhone(result.store().getPhoneNumber()), devCode);
        }
        String token = tokenService.issue(result.store().getId());
        return LoginResponse.authenticated(token, toResponse(result.store()));
    }

    /** Completes a new-device login by verifying the OTP; the device becomes trusted. */
    @PostMapping("/auth/verify-device")
    public AuthResponse verifyDevice(@Valid @RequestBody VerifyDeviceRequest request,
                                     @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId) {
        Store store = authService.verifyDevice(request.username(), request.code(), deviceId);
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
                store, request.storeName(), request.ownerName(), request.phoneNumber());
        return toResponse(updated);
    }

    /** Sends an OTP to the owner's mobile so they can verify they own the number. */
    @PostMapping("/store/phone/verify/request")
    public PhoneVerificationResponse requestPhoneVerification(@CurrentStore Store store) {
        String code = authService.requestPhoneVerification(store);
        String devCode = authService.isLiveSms() ? null : code;
        return new PhoneVerificationResponse(maskPhone(store.getPhoneNumber()), devCode);
    }

    /** Confirms the OTP and marks the owner's mobile number as verified. */
    @PostMapping("/store/phone/verify/confirm")
    public StoreResponse confirmPhoneVerification(
            @CurrentStore Store store,
            @Valid @RequestBody PhoneVerificationRequest request,
            @RequestHeader(value = DEVICE_HEADER, required = false) String deviceId) {
        Store verified = authService.confirmPhoneVerification(store, request.code(), deviceId);
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

    /** Masks a phone number, revealing only the last 4 digits (e.g. +63•••••4567). */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return phone;
        }
        int visible = 4;
        StringBuilder sb = new StringBuilder();
        int keepPrefix = phone.startsWith("+") ? 3 : 0;
        for (int i = 0; i < phone.length(); i++) {
            if (i < keepPrefix || i >= phone.length() - visible) {
                sb.append(phone.charAt(i));
            } else {
                sb.append('•');
            }
        }
        return sb.toString();
    }

    private static StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getUsername(),
                store.getPhoneNumber(),
                store.getName(),
                store.getOwnerName(),
                store.isOnboarded(),
                store.isPhoneVerified(),
                store.hasQrCode());
    }
}
