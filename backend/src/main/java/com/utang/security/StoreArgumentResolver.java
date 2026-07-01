package com.utang.security;

import com.utang.domain.Store;
import com.utang.error.UnauthorizedException;
import com.utang.repository.StoreRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resolves the {@link Store} for the {@code Authorization: Bearer <token>} header. */
@Component
public class StoreArgumentResolver implements HandlerMethodArgumentResolver {

    private final TokenService tokenService;
    private final StoreRepository storeRepository;

    public StoreArgumentResolver(TokenService tokenService, StoreRepository storeRepository) {
        this.tokenService = tokenService;
        this.storeRepository = storeRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentStore.class)
                && Store.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String header = request == null ? null : request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing bearer token");
        }
        String token = header.substring("Bearer ".length()).trim();
        Long storeId = tokenService.resolveStoreId(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid token"));
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new UnauthorizedException("Store not found"));
    }
}
