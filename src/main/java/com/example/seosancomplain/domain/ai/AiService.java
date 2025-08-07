package com.example.seosancomplain.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

    private static final String AI_SERVER_URL = "http://localhost:8000/api/classify";

    public String classifyComplaint(String content, String imageUrl) {
        return "ENVIRONMENT";
    }
}
