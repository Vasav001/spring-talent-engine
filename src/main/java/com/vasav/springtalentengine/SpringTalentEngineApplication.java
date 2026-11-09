package com.vasav.springtalentengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SpringTalentEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringTalentEngineApplication.class, args);
    }

}
