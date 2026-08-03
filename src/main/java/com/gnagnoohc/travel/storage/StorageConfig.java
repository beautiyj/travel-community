package com.gnagnoohc.travel.storage;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 이미지 저장소 공용 설정. 배포에서 넣어야 하는 환경변수는
 * application.properties의 app.storage 주석에 정리해 두었다.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    /**
     * Cloudinary는 cloudinary://{api_key}:{api_secret}@{cloud_name} URL 하나로 인증이 끝난다.
     * 로컬은 application-secret.properties의 cloudinary.url,
     * 배포는 CLOUDINARY_URL 환경변수를 그대로 쓴다(스프링 완화된 바인딩으로 같은 키에 매핑된다).
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "cloudinary")
    public Cloudinary cloudinary(@Value("${cloudinary.url:}") String cloudinaryUrl) {
        if (cloudinaryUrl.isBlank()) {
            throw new IllegalStateException(
                    "app.storage.type=cloudinary 인데 cloudinary.url(또는 CLOUDINARY_URL 환경변수)이 없습니다.");
        }
        return new Cloudinary(cloudinaryUrl);
    }

    /**
     * serve-local=true인 bucket의 로컬 폴더를 url-prefix로 서빙한다.
     * 각 도메인이 이미 자기 리소스 핸들러를 등록해 둔 경우 중복되므로 기본값은 false이고,
     * 기존 핸들러를 지우면서 옮겨올 때 true로 켜면 된다.
     */
    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
    public WebMvcConfigurer localImageResourceConfigurer(StorageProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                properties.getBuckets().values().stream()
                        .filter(StorageProperties.Bucket::isServeLocal)
                        .forEach(bucket -> registry
                                .addResourceHandler(bucket.getUrlPrefix() + "/**")
                                .addResourceLocations(bucket.rootPath().toUri().toString()));
            }
        };
    }
}
