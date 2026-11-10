package snovasys.ledger.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${whaths-app-base.base}")
    private String whathsappUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(whathsappUrl)
                .build();
    }
}