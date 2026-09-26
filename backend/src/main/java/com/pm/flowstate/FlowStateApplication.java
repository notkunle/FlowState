package com.pm.flowstate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlowStateApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowStateApplication.class, args);
    }

}
