# syntax=docker/dockerfile:1

# --- 1) 빌드 스테이지: Gradle로 bootJar 생성 ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 레이어 캐시: 빌드 스크립트/래퍼 먼저 복사
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# 소스 복사 후 실행 가능 jar 빌드 (테스트는 배포 빌드에서 제외)
COPY . .
RUN ./gradlew --no-daemon clean bootJar -x test

# --- 2) 실행 스테이지: 경량 JRE ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# postgres 프로파일로 기동 (DB_URL/DB_USER/DB_PASSWORD 는 Railway 환경변수로 주입)
ENV SPRING_PROFILES_ACTIVE=postgres

COPY --from=build /app/build/libs/*.jar app.jar

# Railway 가 PORT 를 주입하며, application.yml 의 server.port=${PORT:8080} 가 이를 사용
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
