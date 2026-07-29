package com.gnagnoohc.travel.mypage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MypageWebConfig implements WebMvcConfigurer {

    private final MypageAccessInterceptor mypageAccessInterceptor;

    public MypageWebConfig(
            MypageAccessInterceptor mypageAccessInterceptor) {
        this.mypageAccessInterceptor = mypageAccessInterceptor;
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
    }
}
