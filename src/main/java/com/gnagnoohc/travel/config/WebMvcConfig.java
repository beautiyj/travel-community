package com.gnagnoohc.travel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.upload-community}")
    private String uploadCommunityDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/place/**")
                .addResourceLocations(uploadPath.toUri().toString());

        // 커뮤니티 게시글 이미지 (CommunityController.saveImages() 가 저장하는 위치)
        Path uploadCommunityPath = Paths.get(uploadCommunityDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(uploadCommunityPath.toUri().toString());
    }
}
