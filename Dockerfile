# --- 1단계: 빌드 ---
FROM gradle:8.8-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle gradle
COPY src src
RUN gradle bootJar --no-daemon

# --- 2단계: 런타임 ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# 빌드된 JAR 복사 (이 패턴은 build/libs 안의 단일 JAR을 잡습니다)
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar","--server.address=0.0.0.0","--server.port=8080"]
