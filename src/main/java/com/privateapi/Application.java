package com.privateapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@SpringBootApplication
@EnableAsync
public class Application{


    @Autowired
    private Environment environment;

    public static void main(String[] args) {

        /*
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", "7890");
            System.setProperty("https.proxyHost", "127.0.0.1");
            System.setProperty("https.proxyPort", "7890");
        */

        SpringApplication.run(Application.class, args);
    }
}
