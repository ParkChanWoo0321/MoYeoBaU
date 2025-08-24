// src/main/java/com/example/seosancomplain/ai/AiMinwonClient.java
package com.example.seosancomplain.domain.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMinwonClient {

    private final WebClient aiMinwonWebClient;

    /** JSON 방식 호출: /ai/minwon/compose */
    public AiComposeOut compose(AiComposeIn in) {
        try {
            return aiMinwonWebClient.post()
                    .uri("/ai/minwon/compose")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(in)
                    .retrieve()
                    .bodyToMono(AiComposeOut.class)
                    .block(Duration.ofSeconds(60));
        } catch (WebClientResponseException e) {
            log.warn("[AI] compose HTTP {}: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new AiComposeException("AI 서버 응답 오류: " + e.getRawStatusCode(), e);
        } catch (Exception e) {
            log.error("[AI] compose 실패", e);
            throw new AiComposeException("AI 서버 호출 실패", e);
        }
    }

    /** (선택) 멀티파트 폼 호출: /ai/minwon/compose-form */
    public AiComposeOut composeForm(AiComposeForm in) {
        try {
            var form = new LinkedMultiValueMap<String, Object>();

            if (in.getApplicantName() != null)   form.add("applicant_name", in.getApplicantName());
            if (in.getApplicantPhone() != null)  form.add("applicant_phone", in.getApplicantPhone());
            if (in.getApplicantAddress() != null)form.add("applicant_address", in.getApplicantAddress());
            form.add("complaint_text", in.getComplaintText() != null ? in.getComplaintText() : "");
            if (in.getMetaJson() != null)        form.add("meta", in.getMetaJson());

            if (in.getFiles() != null) {
                for (var f : in.getFiles()) {
                    var partBuilder = new org.springframework.http.client.MultipartBodyBuilder();
                    partBuilder.part("files", f.getContent())
                            .filename(f.getFilename())
                            .contentType(f.getMediaType() != null ? f.getMediaType() : MediaType.APPLICATION_OCTET_STREAM);
                    form.add("files", partBuilder.build().toSingleValueMap().get("files"));
                }
            }

            return aiMinwonWebClient.post()
                    .uri("/ai/minwon/compose-form")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(AiComposeOut.class)
                    .block(Duration.ofSeconds(30));
        } catch (WebClientResponseException e) {
            log.warn("[AI] compose-form HTTP {}: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new AiComposeException("AI 서버 응답 오류(멀티파트): " + e.getRawStatusCode(), e);
        } catch (Exception e) {
            log.error("[AI] compose-form 실패", e);
            throw new AiComposeException("AI 서버 호출 실패(멀티파트)", e);
        }
    }

    /* ======================= DTOs ======================= */

    @Data
    @Builder
    public static class AiComposeIn {
        /** 사용자가 적은 원문 (없으면 빈 문자열) */
        @JsonProperty("complaint_text")
        private String complaintText;

        /** 이미지 입력 (URL만 있어도 됨) */
        private List<ImageInput> images;

        /** 메타(비우면 FastAPI가 서산 기본값 주입) */
        private Map<String, Object> meta;
    }

    @Data
    @Builder
    public static class ImageInput {
        private String url;
        @JsonProperty("mime_type")
        private String mimeType;   // optional
        private String filename;   // optional

        public static ImageInput fromUrl(String url) {
            return ImageInput.builder().url(url).build();
        }
    }

    /** 멀티파트 폼 입력용 */
    @Data
    @Builder
    public static class AiComposeForm {
        private String applicantName;
        private String applicantPhone;
        private String applicantAddress;
        private String complaintText; // default ""
        /** {"org":"서산시청","receiver":"서산시청장 귀하","title_prefix":"[서산시]"} */
        private String metaJson;
        private List<FilePart> files;

        @Data
        @Builder
        public static class FilePart {
            private String filename;
            private MediaType mediaType; // image/png 등
            private byte[] content;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiComposeOut {
        //제목
        private String title;

        // 본문(우선순위: html → markdown → body)
        @JsonProperty("doc_html")
        private String docHtml;

        @JsonProperty("doc_markdown")
        private String docMarkdown;

        private String body;

        // 추출 필드(주소/시간/카테고리 후보 등)
        private JsonNode fields;

        // 최종 메타(서산 기본 포함 가능)
        private JsonNode meta;

        @JsonProperty("raw")
        private JsonNode raw;
    }

    public static class AiComposeException extends RuntimeException {
        public AiComposeException(String message) { super(message); }
        public AiComposeException(String message, Throwable cause) { super(message, cause); }
    }
}
