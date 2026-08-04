package com.gnagnoohc.travel.mypage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gnagnoohc.travel.mypage.service.BusinessMediaStorage;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    private final BusinessMediaStorage mediaStorage;

    public MediaResourceConfig(BusinessMediaStorage mediaStorage) {
        this.mediaStorage = mediaStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/mypage/**")
                .addResourceLocations(
                        mediaStorage.getRootDirectory().toUri().toString());
    }
}
