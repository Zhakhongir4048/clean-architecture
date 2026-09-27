package org.esonov.clean_architecture;

import org.springframework.boot.SpringApplication;

public class TestCleanArchitectureApplication {

    public static void main(String[] args) {
        SpringApplication.from(CleanArchitectureApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
