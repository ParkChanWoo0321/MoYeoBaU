package com.example.seosancomplain.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS 설정: 개발환경은 모두 허용, 배포시 도메인 제한 가능
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")        // 모든 경로에 대해
                .allowedOrigins("*")      // 모든 Origin 허용 (운영 시는 실제 프론트 도메인으로 제한!)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    // 정적 자원(파일 업로드 경로) 매핑 예시
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 예: /uploads/** 로 접근하면 실제 로컬 uploads 폴더 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    // 메시지 컨버터, 인터셉터 등 추가 설정 필요시 여기에 구현
}
