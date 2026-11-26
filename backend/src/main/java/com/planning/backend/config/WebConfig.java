package com.planning.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.planning.backend.security.ActivityTracker;
import com.planning.backend.service.ImageStorageService;

// Віддає збережені фото з папки uploads за адресою /uploads/<підпапка>/<ім'я файлу>
// і підключає ActivityTracker (час останньої дії користувача для адмінки)
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ImageStorageService imageStorage;
    private final ActivityTracker activityTracker;

    public WebConfig(ImageStorageService imageStorage, ActivityTracker activityTracker) {
        this.imageStorage = imageStorage;
        this.activityTracker = activityTracker;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activityTracker).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ImageStorageService.URL_PREFIX + "**")
                .addResourceLocations(imageStorage.getRoot().toUri().toString());
    }
}
