package com.gnagnoohc.travel.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

// 패키지명(.mapper/.repository 등)에 의존하지 않고 @Mapper 애노테이션 기준으로 스캔한다.
//todo: myPage 측에서 컨벤션 통일 시, 주석처리된 내용으로 교체후 코드 삭제 예정
@Configuration
@MapperScan(basePackages = "com.gnagnoohc.travel", annotationClass = Mapper.class)
//@MapperScan(basePackages = "com.gnagnoohc.travel.**.mapper")
public class MyBatisConfig { }