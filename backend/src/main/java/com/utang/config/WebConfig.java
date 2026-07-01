package com.utang.config;

import com.utang.security.StoreArgumentResolver;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final StoreArgumentResolver storeArgumentResolver;

    public WebConfig(StoreArgumentResolver storeArgumentResolver) {
        this.storeArgumentResolver = storeArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(storeArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Frontend dev server; tighten allowed origins for production deployments.
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
