package com.gnagnoohc.travel.mypage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MypageWebConfig implements WebMvcConfigurer {

    private final MypageAccessInterceptor mypageAccessInterceptor;
    private final BusinessLoginRedirectInterceptor businessLoginRedirectInterceptor;

    public MypageWebConfig(
            MypageAccessInterceptor mypageAccessInterceptor,
            BusinessLoginRedirectInterceptor businessLoginRedirectInterceptor) {
        this.mypageAccessInterceptor = mypageAccessInterceptor;
        this.businessLoginRedirectInterceptor = businessLoginRedirectInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(mypageAccessInterceptor)
                .addPathPatterns(
                        "/mypage/info",
                        "/mypage/edit",
                        "/mypage/reservation",
                        "/mypage/payment",
                        "/mypage/withdraw",
                        "/mypage/wishlist");

        registry.addInterceptor(businessLoginRedirectInterceptor)
                .addPathPatterns("/auth/login");
    }
}
