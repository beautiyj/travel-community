package com.gnagnoohc.travel.storage;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 이미지 저장소 공용 설정
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
    public Cloudinary cloudinary(@Value("${cloudinary.url:}") String cloudinaryUrl) {
        if (cloudinaryUrl.isBlank()) {
            throw new IllegalStateException(
                    "cloudinary.url(또는 CLOUDINARY_URL 환경변수)이 없습니다.");
        }
        return new Cloudinary(cloudinaryUrl);
    }
}
