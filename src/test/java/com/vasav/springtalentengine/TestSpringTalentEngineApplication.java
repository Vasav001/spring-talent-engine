package com.vasav.springtalentengine;

import org.springframework.boot.SpringApplication;

public class TestSpringTalentEngineApplication {

    public static void main(String[] args) {
        SpringApplication.from(SpringTalentEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
