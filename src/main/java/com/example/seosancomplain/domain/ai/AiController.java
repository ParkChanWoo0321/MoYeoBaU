package com.example.seosancomplain.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private final AiClient ai;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", ai.health());
    }

    // 요청: { "text": "...", "maxSentences": 3 }  (maxSentences 생략 가능)
    @PostMapping("/summarize")
    public Map<String, String> summarize(@RequestBody SummarizeReq req) {
        int k = req.maxSentences == null ? 3 : req.maxSentences;
        String summary = ai.summarize(req.text, k);
        return Map.of("summary", summary);
    }

    public record SummarizeReq(String text, Integer maxSentences) {}
}
