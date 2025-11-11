package de.seuhd.campuscoffee;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application configuration class.
 */
@Configuration
public class AppConfig {

    /**
     * Configures a RestTemplate bean for making HTTP requests.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}