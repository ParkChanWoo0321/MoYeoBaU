package com.example.seosancomplain.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/classify")
    public ResponseEntity<String> classify(@RequestParam String content, @RequestParam(required = false) String imageUrl) {
        String category = aiService.classifyComplaint(content, imageUrl);
        return ResponseEntity.ok(category);
    }
}
