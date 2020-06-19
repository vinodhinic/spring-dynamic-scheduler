package com.foo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class FooApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("EST5EDT"));
        SpringApplication.run(FooApplication.class, args);
    }
}