package com.example.seosancomplain.domain.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {

    // FastAPI 등 외부 AI 서버 주소
    private static final String AI_SERVER_URL = "http://localhost:8000/api/classify";

    // 민원 텍스트, 이미지 → 카테고리 자동 분류
    public String classifyComplaint(String content, String imageUrl) {
        // 실제 구현은 AI 서버에 HTTP POST/GET 요청
        // 여기서는 간단 예시로 반환
        // (실제는 RestTemplate/WebClient 등 사용)
        // 예: { "content": "가로등이 고장났어요", "imageUrl": "..." }
        return "ENVIRONMENT";
    }

    // 민원 요약, 유사 민원 클러스터링 등도 추가 가능
}
