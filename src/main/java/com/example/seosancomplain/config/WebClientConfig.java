package com.example.seosancomplain.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${ai.summarizer.base-url:http://127.0.0.1:8000}")
    String baseUrl;

    @Value("${ai.timeout.connect-ms:999999999}")
    int connectTimeoutMs;

    @Value("${ai.timeout.response-ms:999999999}")
    int responseTimeoutMs;

    @Value("${ai.timeout.read-secs:999999999}")
    int readTimeoutSecs;

    @Value("${ai.timeout.write-secs:999999999}")
    int writeTimeoutSecs;

    @Value("${spring.codec.max-in-memory-size-bytes:16777216}")
    int maxInMemoryBytes;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutSecs, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutSecs, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(baseUrl)

                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(maxInMemoryBytes))
                        .build())
                .build();
    }
}