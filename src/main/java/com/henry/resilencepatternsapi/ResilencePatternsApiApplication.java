package com.henry.resilencepatternsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.henry.resilencepatternsapi",
        "com.henry.resiliencepatternsapi"
})
public class ResilencePatternsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResilencePatternsApiApplication.class, args);
    }

}
