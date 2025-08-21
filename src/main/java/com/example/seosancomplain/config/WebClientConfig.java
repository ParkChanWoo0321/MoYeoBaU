package com.example.seosancomplain.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.unit.DataSize;
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
    @Bean(name = {"aiMinwonWebClient", "webClient"})
    public WebClient aiMinwonWebClient() {

        // HttpClient 설정
        HttpClient httpClient = HttpClient.create()
                .compress(true) // gzip/deflate
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutSecs, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutSecs, TimeUnit.SECONDS))
                );

        // buffer 사이즈 파싱 (DataSize 사용)
        int maxBytes = parseMaxInMemoryBytes(maxInMemorySize);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(maxBytes))
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                // 기본 Accept를 JSON으로(필요 시 개별 호출에서 override)
                .defaultHeaders(h -> h.set("Accept", "application/json"))
                .build();
    }

    private int parseMaxInMemoryBytes(String size) {
        try {
            DataSize ds = DataSize.parse(size);   // "16MB", "8MB", "2048KB", "1048576B", "1048576"
            long bytes = ds.toBytes();
            if (bytes > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            return (int) bytes;
        } catch (Exception ignore) {
            return 16 * 1024 * 1024; // fallback 16MB
        }
    }
}
