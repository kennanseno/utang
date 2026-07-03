package com.utang.web;

import com.utang.domain.Store;
import com.utang.dto.Dtos.AuthResponse;
import com.utang.dto.Dtos.OnboardingRequest;
import com.utang.dto.Dtos.RequestOtpRequest;
import com.utang.dto.Dtos.RequestOtpResponse;
import com.utang.dto.Dtos.StoreResponse;
import com.utang.dto.Dtos.VerifyOtpRequest;
import com.utang.security.CurrentStore;
import com.utang.security.TokenService;
import com.utang.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/auth/request-otp")
    public RequestOtpResponse requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        String code = authService.requestOtp(request.phoneNumber());
        if (authService.isLiveSms()) {
            return new RequestOtpResponse(request.phoneNumber(), null, "OTP sent via SMS");
        }
        // No SMS gateway configured — echo the code so it can be used for testing.
        return new RequestOtpResponse(request.phoneNumber(), code, "OTP generated (dev mode)");
    }

    @PostMapping("/auth/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        Store store = authService.verifyOtp(request.phoneNumber(), request.code());
        String token = tokenService.issue(store.getId());
        return new AuthResponse(token, store.isOnboarded(), toResponse(store));
    }

    /** Completes onboarding for the authenticated store owner. */
    @PostMapping("/onboarding")
    public StoreResponse onboard(@CurrentStore Store store, @Valid @RequestBody OnboardingRequest request) {
        Store updated = authService.onboard(store, request.storeName(), request.ownerName());
        return toResponse(updated);
    }

    @GetMapping("/me")
    public StoreResponse me(@CurrentStore Store store) {
        return toResponse(store);
    }

    private static StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getPhoneNumber(),
                store.getName(),
                store.getOwnerName(),
                store.isOnboarded());
    }
}
