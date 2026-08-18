package com.travelshare.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.travelshare.platform.mapper")
@SpringBootApplication
public class TravelShareApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelShareApplication.class, args);
    }
}

