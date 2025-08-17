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

    @Value("${ai.base-url:${AI_BASE_URL:http://ai:8000}}")
    String baseUrl;

    @Value("${ai.timeout.connect-ms:999999999}")
    int connectTimeoutMs;

    @Value("${ai.timeout.response-ms:999999999}")
    int responseTimeoutMs;

    @Value("${ai.timeout.read-secs:999999999}")
    int readTimeoutSecs;

    @Value("${ai.timeout.write-secs:999999999}")
    int writeTimeoutSecs;

    @Value("${spring.codec.max-in-memory-size:16MB}")
    String maxInMemorySize;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutSecs, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutSecs, TimeUnit.SECONDS))
                );

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> {
                    int bytes;
                    try {
                        String v = maxInMemorySize.trim().toUpperCase();
                        if (v.endsWith("MB")) {
                            bytes = Integer.parseInt(v.replace("MB", "").trim()) * 1024 * 1024;
                        } else if (v.endsWith("KB")) {
                            bytes = Integer.parseInt(v.replace("KB", "").trim()) * 1024;
                        } else if (v.endsWith("B")) {
                            bytes = Integer.parseInt(v.replace("B", "").trim());
                        } else {
                            bytes = Integer.parseInt(v);
                        }
                    } catch (Exception e) {
                        bytes = 16 * 1024 * 1024;
                    }
                    c.defaultCodecs().maxInMemorySize(bytes);
                })
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
