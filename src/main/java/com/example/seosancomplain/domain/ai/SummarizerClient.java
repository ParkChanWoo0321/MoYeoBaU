package com.example.seosancomplain.domain.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummarizerClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    @Value("${ai.summarizer.endpoint:/summarize}")
    private String endpoint;

    @Value("${ai.timeout.response-ms:120000}")
    private long clientTimeoutMs;

    public Mono<FieldsResponse> summarizeFieldsJson(String text, List<String> imageUrls) {
        SummarizeRequest req = new SummarizeRequest();
        String safe = text == null ? "" : text.trim();
        req.setComplaint_text(safe);
        if (imageUrls != null) {
            imageUrls.forEach(url -> req.getImages().add(new ImageInput(url, null)));
        }
        Map<String, Object> meta = new HashMap<>();
        meta.put("response", "json");
        meta.put("keys", "ko");
        req.setMeta(meta);


        if (log.isDebugEnabled()) {
            try { log.debug("[AI→JSON] POST {}{} payload={}", aiBaseUrl, endpoint, objectMapper.writeValueAsString(req)); }
            catch (Exception ignore) {}
        }

        return webClient.post()
                .uri(aiBaseUrl + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.createException().flatMap(Mono::error))
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(clientTimeoutMs))
                .map(raw -> {
                    try {
                        return objectMapper.readValue(raw, FieldsResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("AI 응답 파싱 실패: " + raw, e);
                    }
                });
    }

    public Mono<FieldsResponse> summarizeFieldsMultipart(String text, List<byte[]> imageBytesList) {
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        String safe = text == null ? "" : text.trim();
        mb.part("complaint_text", safe);
        if (imageBytesList != null) {
            int idx = 0;
            for (byte[] bytes : imageBytesList) {
                var res = new ByteArrayResource(bytes) {
                    @Override public String getFilename() { return "img_" + System.nanoTime() + ".jpg"; }
                };
                mb.part("images", res)
                        .contentType(MediaType.IMAGE_JPEG)
                        .filename("complaint_" + (idx++) + ".jpg");
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[AI→MULTIPART] POST {}{} text.len={}", aiBaseUrl, endpoint, safe.length());
        }

        return webClient.post()
                .uri(aiBaseUrl + endpoint)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(mb.build()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.createException().flatMap(Mono::error))
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(clientTimeoutMs))
                .map(raw -> {
                    try {
                        return objectMapper.readValue(raw, FieldsResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("AI 응답 파싱 실패: " + raw, e);
                    }
                });
    }

    @Data
    public static class SummarizeRequest {
        private String complaint_text = "";
        private List<ImageInput> images = new ArrayList<>();
        private Map<String, Object> meta;
    }

    @Data
    @Builder
    public static class ImageInput {
        private String url;
        private String base64;
        public ImageInput(String url, String base64) { this.url = url; this.base64 = base64; }
    }

    @Data
    public static class FieldsResponse {
        @JsonAlias({"location","위치"})
        private String location;
        @JsonAlias({"phenomenon","현상"})
        private String phenomenon;
        @JsonAlias({"problem","문제점"})
        private String problem;
        @JsonAlias({"risk","위험성"})
        private String risk;
        @JsonAlias({"request","요청사항"})
        private String request;
    }
}