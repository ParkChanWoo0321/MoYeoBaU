package com.example.seosancomplain.domain.ai;

import com.example.seosancomplain.domain.ai.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    private final WebClient webClient;
    @Autowired
    public AiClient(WebClient.Builder builder,
                    @Value("${ai.base-url:http://localhost:8000}") String baseUrl) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** 요약 (기본 3문장) */
    public String summarize(String text) {
        return summarize(text, 3);
    }

    /** 요약 (문장 수 지정) */
    public String summarize(String text, int maxSentences) {
        Map<String, Object> req = Map.of("text", text, "max_sentences", maxSentences);

        Map<String, Object> res = postJson(
                "/ai/summarize",
                req,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                DEFAULT_TIMEOUT
        );

        Object summary = res.get("summary");
        return summary == null ? "" : String.valueOf(summary);
    }

    /** 헬스 체크 (FastAPI의 /health 기준) */
    public boolean health() {
        try {
            Map<String, Object> res = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r ->
                            r.bodyToMono(String.class)
                                    .map(body -> new IllegalStateException("AI health HTTP " + r.statusCode().value() + ": " + body)))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(5));

            return res != null && Boolean.TRUE.equals(res.get("ok"));
        } catch (Exception e) {
            log.warn("AI health check failed: {}", e.toString());
            return false;
        }
    }

    /* -------------------- 내부 공용 메서드 -------------------- */

    private <T> T postJson(String path, Object body,
                           ParameterizedTypeReference<T> type, Duration timeout) {
        return webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class)
                                .map(msg -> new IllegalStateException("AI HTTP " + r.statusCode().value() + ": " + msg)))
                .bodyToMono(type)
                .block(timeout);
    }

    /* -------------------- 민원 전용 API ----------------------*/
    /** 필드 추출: POST /ai/minwon/prepare  */
    public ComplaintFields prepare(SummarizeRequest req) {
        return postJson(
                "/ai/minwon/prepare",
                req,
                new ParameterizedTypeReference<ComplaintFields>() {},
                DEFAULT_TIMEOUT
        );
    }

    /** 문서화(제목/본문/HTML 생성): POST /ai/minwon/compose */
    public ComposeOut compose(ComposeIn req) {
        return postJson(
                "/ai/minwon/compose",
                req,
                new ParameterizedTypeReference<ComposeOut>() {},
                DEFAULT_TIMEOUT
        );
    }
}
