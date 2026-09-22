package com.motaamneh.mstsocialmvp;

import org.springframework.boot.SpringApplication;

public class TestMstSocialMvpApplication {

    public static void main(String[] args) {
        SpringApplication.from(MstSocialMvpApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
