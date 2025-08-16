package com.example.seosancomplain.domain.ai;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class PingController {
    @GetMapping("/healthz")
    public Map<String, String> healthz() {
        return Map.of("status","ok");
    }
}