package com.mongodemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContextAware;

@SpringBootApplication
@MapperScan("com.mongodemo.dao")
public class MongodemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongodemoApplication.class, args);
    }

}
