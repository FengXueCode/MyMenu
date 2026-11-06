package com.fengxue.menu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web 配置：鉴权拦截器、本地图片映射、开发期 CORS。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String uploadDir;
    private final String[] allowedOrigins;

    public WebConfig(AuthInterceptor authInterceptor,
                     @Value("${app.storage.upload-dir:data/uploads}") String uploadDir,
                     @Value("${app.cors.allowed-origin-patterns:}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.uploadDir = uploadDir;
        this.allowedOrigins = allowedOrigins.isBlank() ? new String[0] : allowedOrigins.split(",");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/health",
                        "/api/health/**",
                        "/api/files/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Request-Id")
                .maxAge(3600);
    }
}
