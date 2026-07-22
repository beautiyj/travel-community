package com.gnagnoohc.travel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // 예약 만료 스케줄러(@Scheduled) 활성화
@MapperScan("com.gnagnoohc.travel.tour.**.mapper")   // tour 파트 매퍼 스캔 (yun)
public class TravelApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelApplication.class, args);
	}
}
