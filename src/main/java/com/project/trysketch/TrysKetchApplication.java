package com.project.trysketch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class TrysKetchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrysKetchApplication.class, args);
    }

}
