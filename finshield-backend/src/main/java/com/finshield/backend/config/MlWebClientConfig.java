package com.finshield.backend.config;

import com.finshield.backend.risk.ml.MlClientProperties;
import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(MlClientProperties.class)
public class MlWebClientConfig {

    @Bean
    WebClient finshieldMlWebClient(MlClientProperties properties, WebClient.Builder builder) {
        int timeoutMillis = Math.toIntExact(properties.timeout().toMillis());
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
                .responseTimeout(properties.timeout());

        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
