package com.jiburo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class JiburoServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiburoServerApplication.class, args);
    }

}
