package com.example.seosancomplain.domain.ai;

import lombok.RequiredArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SummarizerClient {

    private final WebClient webClient;

    @Value("${ai.summarizer.base-url}")
    private String baseUrl;

    @Value("${ai.summarizer.endpoint:/summarize}")
    private String endpoint;

    public Mono<FieldsResponse> summarizeFieldsJson(String text, List<String> imageUrls) {
        SummarizeRequest req = new SummarizeRequest();
        req.setComplaint_text(text == null ? "" : text);
        if (imageUrls != null) {
            for (String url : imageUrls) {
                req.getImages().add(new ImageInput(url, null));
            }
        }
        Map<String, Object> meta = new HashMap<>();
        meta.put("response", "json");
        meta.put("keys", "en"); // 🔹영문 키 요청
        req.setMeta(meta);

        return webClient.post()
                .uri(baseUrl + endpoint + "?format=json&keys=en") // 🔹영문 키 요청
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(FieldsResponse.class);
    }

    public Mono<FieldsResponse> summarizeFieldsMultipart(String text, List<byte[]> imageBytesList) {
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("complaint_text", text == null ? "" : text);

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

        return webClient.post()
                .uri(baseUrl + endpoint + "?format=json&keys=en") // 🔹영문 키 요청
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromMultipartData(mb.build()))
                .retrieve()
                .bodyToMono(FieldsResponse.class);
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
        private String location;
        private String phenomenon;
        private String problem;
        private String risk;
        private String request;
    }
}
