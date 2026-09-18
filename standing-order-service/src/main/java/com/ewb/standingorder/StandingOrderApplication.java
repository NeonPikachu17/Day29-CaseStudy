package com.ewb.standingorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StandingOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(StandingOrderApplication.class, args);
    }
}
