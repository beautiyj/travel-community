package com.gnagnoohc.travel.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.gnagnoohc.travel.**.mapper")
public class MyBatisConfig { }