package com.motaamneh.mstsocialmvp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MstSocialMvpApplication {

    public static void main(String[] args) {
        SpringApplication.run(MstSocialMvpApplication.class, args);
    }

}
