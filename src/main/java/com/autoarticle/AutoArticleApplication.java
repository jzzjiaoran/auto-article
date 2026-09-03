package com.autoarticle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AutoArticleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoArticleApplication.class, args);
    }
}
