package com.tanuj.peerskillexchange.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BoardJsonConfig {
    @Bean
    ObjectMapper boardObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
