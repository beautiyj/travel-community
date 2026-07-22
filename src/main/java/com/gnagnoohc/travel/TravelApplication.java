package com.gnagnoohc.travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

// 0716 yun 브랜치 서버 구동용 로직 추가 - 다른 브랜치에서는 해당 코드 제거/수정 후 테스트 권장
@SpringBootApplication
@ComponentScan(
    basePackages = "com.gnagnoohc.travel",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = {
                "com\\.gnagnoohc\\.travel\\.admin\\..*",
                "com\\.gnagnoohc\\.travel\\.auth\\..*",
                "com\\.gnagnoohc\\.travel\\.batch\\..*",
                "com\\.gnagnoohc\\.travel\\.community\\..*",
                "com\\.gnagnoohc\\.travel\\.mypage\\..*",
                "com\\.gnagnoohc\\.travel\\.reservation\\..*"
            }
        )
    }
)
public class TravelApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelApplication.class, args);
	}
}