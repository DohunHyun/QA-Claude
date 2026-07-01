plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.nh"
version = "0.0.1-SNAPSHOT"

java {
    // 사내 표준은 Java 17. JDK 21로 빌드하더라도 17 바이트코드로 컴파일한다.
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Web / REST ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- 영속성 (JPA) ---
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // --- Claude API 연동용 WebClient ---
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // --- API 문서화 (Swagger UI) ---
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // --- 파일 파싱/생성: Excel·PPTX (HWPX는 별도 파서 예정) ---
    implementation("org.apache.poi:poi:5.3.0")
    implementation("org.apache.poi:poi-ooxml:5.3.0")

    // --- DB: 기본 H2(즉시 실행), 운영 PostgreSQL ---
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // --- Lombok (보일러플레이트 축소) ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // --- 테스트 ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
