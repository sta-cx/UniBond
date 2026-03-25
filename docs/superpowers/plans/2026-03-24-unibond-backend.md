# UniBond Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the UniBond Spring Boot backend — a REST API for couples daily quiz, mood sync, and achievement tracking.

**Architecture:** Single Spring Boot 3.x monolith with PostgreSQL + Redis, deployed via Docker Compose on a 2C2G ECS. Feature-based package structure (auth, user, couple, quiz, mood, stats, push). JWT authentication with APNs push notifications.

**Tech Stack:** Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, PostgreSQL 15, Redis 7, Flyway, Pushy (APNs), Spring Mail, Bucket4j (rate limiting)

**Spec:** `docs/superpowers/specs/2026-03-24-unibond-design.md`

---

## File Structure

```
unibond-server/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── src/main/java/com/unibond/
│   ├── UnibondApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security + JWT filter chain
│   │   ├── RedisConfig.java             # Redis connection + serialization
│   │   ├── RateLimitConfig.java         # Bucket4j rate limit beans
│   │   └── WebConfig.java              # CORS, Jackson, timezone
│   ├── common/
│   │   ├── dto/ApiResponse.java         # Standard success response wrapper
│   │   ├── dto/ErrorResponse.java       # Standard error response {code, message, timestamp}
│   │   ├── dto/CursorPage.java          # Cursor pagination wrapper {data, cursor, hasMore}
│   │   ├── exception/BizException.java  # Business exception with error code
│   │   ├── exception/ErrorCode.java     # Enum of all application error codes
│   │   ├── exception/GlobalExceptionHandler.java
│   │   ├── security/JwtProvider.java    # JWT create/validate/parse
│   │   ├── security/JwtAuthFilter.java  # OncePerRequestFilter for JWT
│   │   ├── security/UserPrincipal.java  # Authentication principal
│   │   ├── ratelimit/RateLimitInterceptor.java
│   │   └── util/InputSanitizer.java     # XSS filter + emoji validation
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── dto/AppleLoginRequest.java
│   │   ├── dto/EmailSendRequest.java
│   │   ├── dto/EmailLoginRequest.java
│   │   ├── dto/AuthResponse.java        # {accessToken, refreshToken, user}
│   │   ├── service/AuthService.java
│   │   ├── service/EmailService.java
│   │   └── service/AppleAuthService.java
│   ├── user/
│   │   ├── controller/UserController.java
│   │   ├── dto/ProfileUpdateRequest.java
│   │   ├── dto/UserResponse.java
│   │   ├── entity/User.java
│   │   ├── entity/AuthProvider.java     # Enum: APPLE, EMAIL
│   │   ├── repository/UserRepository.java
│   │   └── service/UserService.java
│   ├── couple/
│   │   ├── controller/CoupleController.java
│   │   ├── dto/BindRequest.java
│   │   ├── dto/CoupleResponse.java
│   │   ├── entity/Couple.java
│   │   ├── entity/CoupleStatus.java     # Enum: ACTIVE, DISSOLVED
│   │   ├── repository/CoupleRepository.java
│   │   └── service/CoupleService.java
│   ├── quiz/
│   │   ├── controller/QuizController.java
│   │   ├── dto/QuizResponse.java
│   │   ├── dto/AnswerRequest.java
│   │   ├── dto/QuizResultResponse.java
│   │   ├── entity/DailyQuiz.java
│   │   ├── entity/QuizAnswer.java
│   │   ├── entity/QuestionPool.java
│   │   ├── entity/QuizType.java         # Enum: BLIND, GUESS, THEME
│   │   ├── entity/GenerationSource.java # Enum: AI, FALLBACK_POOL
│   │   ├── repository/DailyQuizRepository.java
│   │   ├── repository/QuizAnswerRepository.java
│   │   ├── repository/QuestionPoolRepository.java
│   │   ├── service/QuizService.java
│   │   └── service/QuizGenerationService.java
│   ├── mood/
│   │   ├── controller/MoodController.java
│   │   ├── dto/MoodUpdateRequest.java
│   │   ├── dto/MoodResponse.java
│   │   ├── entity/MoodStatus.java
│   │   ├── repository/MoodRepository.java
│   │   └── service/MoodService.java
│   ├── stats/
│   │   ├── controller/StatsController.java
│   │   ├── dto/OverviewResponse.java
│   │   ├── dto/AchievementResponse.java
│   │   ├── dto/WeeklyResponse.java
│   │   ├── entity/DailyStats.java
│   │   ├── entity/DailyStatsId.java     # Composite PK class
│   │   ├── entity/Achievement.java
│   │   ├── entity/AchievementType.java  # Enum of all achievement types
│   │   ├── repository/DailyStatsRepository.java
│   │   ├── repository/AchievementRepository.java
│   │   ├── service/StatsService.java
│   │   └── service/AchievementService.java
│   └── push/
│       ├── service/PushService.java
│       └── service/LiveActivityPushService.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── db/migration/
│   │   └── V1__init_schema.sql
│   └── templates/
│       └── verification-code.html
├── src/test/java/com/unibond/
│   ├── auth/
│   │   ├── controller/AuthControllerTest.java
│   │   └── service/AuthServiceTest.java
│   ├── user/
│   │   └── service/UserServiceTest.java
│   ├── couple/
│   │   ├── controller/CoupleControllerTest.java
│   │   └── service/CoupleServiceTest.java
│   ├── quiz/
│   │   ├── controller/QuizControllerTest.java
│   │   ├── service/QuizServiceTest.java
│   │   └── service/QuizGenerationServiceTest.java
│   ├── mood/
│   │   └── service/MoodServiceTest.java
│   ├── stats/
│   │   └── service/AchievementServiceTest.java
│   └── common/
│       └── security/JwtProviderTest.java
```

---

### Task 1: Project Scaffolding + Docker Compose

**Files:**
- Create: `unibond-server/pom.xml`
- Create: `unibond-server/docker-compose.yml`
- Create: `unibond-server/Dockerfile`
- Create: `unibond-server/src/main/java/com/unibond/UnibondApplication.java`
- Create: `unibond-server/src/main/resources/application.yml`
- Create: `unibond-server/src/main/resources/application-dev.yml`

- [ ] **Step 1: Create pom.xml with all dependencies**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.3</version>
        <relativePath/>
    </parent>
    <groupId>com.unibond</groupId>
    <artifactId>unibond-server</artifactId>
    <version>0.1.0</version>
    <name>unibond-server</name>
    <description>UniBond - Couples Daily Quiz API</description>
    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.6</jjwt.version>
        <pushy.version>0.15.4</pushy.version>
        <bucket4j.version>8.10.1</bucket4j.version>
    </properties>
    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <!-- Data -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <!-- Mail -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <!-- APNs -->
        <dependency>
            <groupId>com.eatthepath</groupId>
            <artifactId>pushy</artifactId>
            <version>${pushy.version}</version>
        </dependency>
        <!-- Rate Limiting -->
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
        <!-- Utility -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create Spring Boot application class**

```java
// src/main/java/com/unibond/UnibondApplication.java
package com.unibond;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnibondApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnibondApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yml**

```yaml
# src/main/resources/application.yml
spring:
  profiles:
    active: dev
  jackson:
    time-zone: UTC
    date-format: yyyy-MM-dd'T'HH:mm:ss'Z'
    default-property-inclusion: non_null
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080

app:
  jwt:
    secret: ${JWT_SECRET:change-me-in-production-this-must-be-at-least-256-bits-long-for-hs256}
    access-token-expiry: 7200000      # 2 hours
    refresh-token-expiry: 2592000000  # 30 days
  invite-code:
    charset: "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    length: 6
    max-retries: 3
```

- [ ] **Step 4: Create application-dev.yml**

```yaml
# src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/unibond
    username: unibond
    password: unibond_dev
  data:
    redis:
      host: localhost
      port: 6379
  mail:
    host: smtp.qq.com
    port: 465
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.ssl.enable: true

logging:
  level:
    com.unibond: DEBUG
    org.hibernate.SQL: DEBUG
```

- [ ] **Step 4b: Create application-prod.yml**

```yaml
# src/main/resources/application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/unibond
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT:465}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.ssl.enable: true

logging:
  level:
    com.unibond: INFO

server:
  port: 8080
```

- [ ] **Step 5: Create docker-compose.yml**

```yaml
# unibond-server/docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: unibond
      POSTGRES_USER: unibond
      POSTGRES_PASSWORD: unibond_dev
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    command: >
      postgres
        -c shared_buffers=128MB
        -c work_mem=4MB
        -c max_connections=30

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 64mb --maxmemory-policy allkeys-lru

volumes:
  pgdata:
```

- [ ] **Step 6: Create Dockerfile**

```dockerfile
# unibond-server/Dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/unibond-server-*.jar app.jar
ENV JAVA_OPTS="-Xmx384m -Xms256m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

- [ ] **Step 7: Start Docker Compose and verify Spring Boot starts**

Run: `cd unibond-server && docker-compose up -d`
Run: `./mvnw spring-boot:run`
Expected: Application starts on port 8080 (will fail at Flyway since no migration yet — that's OK)

- [ ] **Step 8: Commit**

```bash
git add unibond-server/
git commit -m "feat: scaffold Spring Boot project with Docker Compose"
```

---

### Task 2: Database Schema (Flyway Migration)

**Files:**
- Create: `unibond-server/src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 1: Write the migration SQL**

```sql
-- V1__init_schema.sql

-- Enums
CREATE TYPE auth_provider AS ENUM ('APPLE', 'EMAIL');
CREATE TYPE couple_status AS ENUM ('ACTIVE', 'DISSOLVED');
CREATE TYPE quiz_type AS ENUM ('BLIND', 'GUESS', 'THEME');
CREATE TYPE generation_source AS ENUM ('AI', 'FALLBACK_POOL');

-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255),
    nickname VARCHAR(20),
    avatar_url VARCHAR(500),
    auth_provider auth_provider NOT NULL,
    apple_sub VARCHAR(255),
    partner_id BIGINT,
    invite_code VARCHAR(6) NOT NULL,
    device_token VARCHAR(255),
    timezone VARCHAR(50) DEFAULT 'Asia/Shanghai',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_apple_sub UNIQUE (apple_sub),
    CONSTRAINT uk_users_invite_code UNIQUE (invite_code),
    CONSTRAINT fk_users_partner FOREIGN KEY (partner_id) REFERENCES users(id)
);

-- Couples
CREATE TABLE couples (
    id BIGSERIAL PRIMARY KEY,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    anniversary_date DATE,
    bind_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status couple_status NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_couples_user_a FOREIGN KEY (user_a_id) REFERENCES users(id),
    CONSTRAINT fk_couples_user_b FOREIGN KEY (user_b_id) REFERENCES users(id)
);

-- Daily quizzes
CREATE TABLE daily_quizzes (
    id BIGSERIAL PRIMARY KEY,
    quiz_date DATE NOT NULL,
    couple_id BIGINT NOT NULL,
    quiz_type quiz_type NOT NULL,
    theme VARCHAR(50),
    questions JSONB NOT NULL,
    generation_source generation_source NOT NULL DEFAULT 'AI',
    prompt_context JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_daily_quizzes_couple FOREIGN KEY (couple_id) REFERENCES couples(id),
    CONSTRAINT uk_daily_quizzes_couple_date UNIQUE (couple_id, quiz_date)
);

-- Quiz answers
CREATE TABLE quiz_answers (
    id BIGSERIAL PRIMARY KEY,
    daily_quiz_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    couple_id BIGINT NOT NULL,
    answers JSONB NOT NULL,
    partner_guess JSONB,
    score INT,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revealed BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_quiz_answers_quiz FOREIGN KEY (daily_quiz_id) REFERENCES daily_quizzes(id),
    CONSTRAINT fk_quiz_answers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_quiz_answers_couple FOREIGN KEY (couple_id) REFERENCES couples(id),
    CONSTRAINT uk_quiz_answers_quiz_user UNIQUE (daily_quiz_id, user_id)
);

-- Question pool (fallback)
CREATE TABLE question_pool (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    quiz_type quiz_type NOT NULL,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    difficulty INT NOT NULL DEFAULT 1,
    used_count INT NOT NULL DEFAULT 0
);

-- Mood status
CREATE TABLE mood_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    couple_id BIGINT NOT NULL,
    mood_emoji VARCHAR(10) NOT NULL,
    mood_text VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_mood_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_mood_couple FOREIGN KEY (couple_id) REFERENCES couples(id)
);

-- Achievements
CREATE TABLE achievements (
    id BIGSERIAL PRIMARY KEY,
    couple_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    unlocked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_achievements_couple FOREIGN KEY (couple_id) REFERENCES couples(id),
    CONSTRAINT uk_achievements_couple_type UNIQUE (couple_id, type)
);

-- Daily stats
CREATE TABLE daily_stats (
    couple_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    match_score INT NOT NULL DEFAULT 0,
    streak_days INT NOT NULL DEFAULT 0,
    quiz_type_played quiz_type,

    PRIMARY KEY (couple_id, stat_date),
    CONSTRAINT fk_daily_stats_couple FOREIGN KEY (couple_id) REFERENCES couples(id)
);

-- Indexes
CREATE INDEX idx_daily_quizzes_date ON daily_quizzes(quiz_date);
CREATE INDEX idx_quiz_answers_couple ON quiz_answers(couple_id);
CREATE INDEX idx_mood_status_user ON mood_status(user_id);
CREATE INDEX idx_achievements_couple ON achievements(couple_id);
```

- [ ] **Step 2: Start Docker Compose and run the app to verify migration**

Run: `cd unibond-server && docker-compose up -d && ./mvnw spring-boot:run`
Expected: Flyway runs V1__init_schema.sql, app starts without errors

- [ ] **Step 3: Commit**

```bash
git add unibond-server/src/main/resources/db/migration/
git commit -m "feat: add Flyway V1 migration with full schema"
```

---

### Task 3: Common Infrastructure (Error Handling, DTOs, JWT)

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/common/exception/ErrorCode.java`
- Create: `unibond-server/src/main/java/com/unibond/common/exception/BizException.java`
- Create: `unibond-server/src/main/java/com/unibond/common/exception/GlobalExceptionHandler.java`
- Create: `unibond-server/src/main/java/com/unibond/common/dto/ErrorResponse.java`
- Create: `unibond-server/src/main/java/com/unibond/common/dto/ApiResponse.java`
- Create: `unibond-server/src/main/java/com/unibond/common/dto/CursorPage.java`
- Create: `unibond-server/src/main/java/com/unibond/common/security/JwtProvider.java`
- Create: `unibond-server/src/main/java/com/unibond/common/security/JwtAuthFilter.java`
- Create: `unibond-server/src/main/java/com/unibond/common/security/UserPrincipal.java`
- Create: `unibond-server/src/main/java/com/unibond/config/SecurityConfig.java`
- Create: `unibond-server/src/main/java/com/unibond/config/RedisConfig.java`
- Create: `unibond-server/src/main/java/com/unibond/config/WebConfig.java`
- Test: `unibond-server/src/test/java/com/unibond/common/security/JwtProviderTest.java`

- [ ] **Step 1: Write JwtProvider test**

```java
package com.unibond.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
            "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm",
            7200000L,   // 2h access
            2592000000L // 30d refresh
        );
    }

    @Test
    void createAndValidateAccessToken() {
        String token = jwtProvider.createAccessToken(1L);
        assertTrue(jwtProvider.validate(token));
        assertEquals(1L, jwtProvider.getUserId(token));
    }

    @Test
    void createAndValidateRefreshToken() {
        String token = jwtProvider.createRefreshToken(1L);
        assertTrue(jwtProvider.validate(token));
        assertEquals(1L, jwtProvider.getUserId(token));
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertFalse(jwtProvider.validate("invalid.token.here"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd unibond-server && ./mvnw test -pl . -Dtest=JwtProviderTest -f pom.xml`
Expected: FAIL — class not found

- [ ] **Step 3: Implement ErrorCode enum**

```java
package com.unibond.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    AUTH_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "验证码已过期"),
    AUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "验证码错误"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token已过期"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Token无效"),
    COUPLE_ALREADY_BOUND(HttpStatus.CONFLICT, "已绑定情侣"),
    COUPLE_NOT_BOUND(HttpStatus.BAD_REQUEST, "未绑定情侣"),
    INVITE_CODE_INVALID(HttpStatus.BAD_REQUEST, "邀请码无效"),
    INVITE_CODE_SELF(HttpStatus.BAD_REQUEST, "不能绑定自己"),
    QUIZ_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "今日题目尚未生成"),
    QUIZ_ALREADY_ANSWERED(HttpStatus.CONFLICT, "已回答过"),
    QUIZ_NOT_REVEALED(HttpStatus.BAD_REQUEST, "结果未揭晓"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "请求频率超限"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "用户不存在"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "无权限访问");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
```

- [ ] **Step 4: Implement BizException, ErrorResponse, ApiResponse, CursorPage**

```java
// BizException.java
package com.unibond.common.exception;

public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
```

```java
// ErrorResponse.java
package com.unibond.common.dto;

import java.time.Instant;

public record ErrorResponse(String code, String message, Instant timestamp) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now());
    }
}
```

```java
// ApiResponse.java
package com.unibond.common.dto;

public record ApiResponse<T>(T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data);
    }
}
```

```java
// CursorPage.java
package com.unibond.common.dto;

import java.util.List;

public record CursorPage<T>(List<T> data, String cursor, boolean hasMore) {}
```

- [ ] **Step 5: Implement GlobalExceptionHandler**

```java
package com.unibond.common.exception;

import com.unibond.common.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBiz(BizException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getStatus())
            .body(ErrorResponse.of(code.name(), code.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .findFirst().orElse("参数错误");
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of("VALIDATION_ERROR", msg));
    }
}
```

- [ ] **Step 5b: Implement InputSanitizer utility**

```java
package com.unibond.common.util;

import java.util.regex.Pattern;

public final class InputSanitizer {
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "<[^>]*>|javascript:|on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
        "[\\p{So}\\p{Sk}\\p{Sc}\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}" +
        "\\x{1F680}-\\x{1F6FF}\\x{1F900}-\\x{1F9FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}]+");

    private InputSanitizer() {}

    /** Strip HTML tags and XSS patterns, then truncate to maxLen */
    public static String sanitizeText(String input, int maxLen) {
        if (input == null) return null;
        String clean = XSS_PATTERN.matcher(input).replaceAll("");
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }

    /** Validate that input contains only emoji characters */
    public static boolean isValidEmoji(String input) {
        if (input == null || input.isBlank()) return false;
        return EMOJI_PATTERN.matcher(input).matches();
    }
}
```

```java
package com.unibond.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtProvider {
    private final SecretKey key;
    private final long accessExpiry;
    private final long refreshExpiry;

    public JwtProvider(String secret, long accessExpiry, long refreshExpiry) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiry = accessExpiry;
        this.refreshExpiry = refreshExpiry;
    }

    public String createAccessToken(Long userId) {
        return buildToken(userId, accessExpiry, "access");
    }

    public String createRefreshToken(Long userId) {
        return buildToken(userId, refreshExpiry, "refresh");
    }

    private String buildToken(Long userId, long expiry, String type) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", type)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiry))
            .signWith(key)
            .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
```

- [ ] **Step 7: Implement UserPrincipal and JwtAuthFilter**

```java
// UserPrincipal.java
package com.unibond.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public record UserPrincipal(Long userId) implements UserDetails {
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public String getPassword() { return ""; }
    @Override public String getUsername() { return userId.toString(); }
}
```

```java
// JwtAuthFilter.java
package com.unibond.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtProvider.validate(token)) {
                Long userId = jwtProvider.getUserId(token);
                var principal = new UserPrincipal(userId);
                var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
```

- [ ] **Step 8: Implement SecurityConfig, RedisConfig, WebConfig**

```java
// SecurityConfig.java
package com.unibond.config;

import com.unibond.common.security.JwtAuthFilter;
import com.unibond.common.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtProvider jwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry}") long accessExpiry,
            @Value("${app.jwt.refresh-token-expiry}") long refreshExpiry) {
        return new JwtProvider(secret, accessExpiry, refreshExpiry);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtProvider jwtProvider) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

```java
// RedisConfig.java
package com.unibond.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

```java
// WebConfig.java
package com.unibond.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

- [ ] **Step 9: Run JWT test to verify it passes**

Run: `cd unibond-server && ./mvnw test -Dtest=JwtProviderTest`
Expected: PASS — all 3 tests green

- [ ] **Step 10: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add common infrastructure (JWT, error handling, security config)"
```

---

### Task 4: User Entity + Repository

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/user/entity/AuthProvider.java`
- Create: `unibond-server/src/main/java/com/unibond/user/entity/User.java`
- Create: `unibond-server/src/main/java/com/unibond/user/repository/UserRepository.java`

- [ ] **Step 1: Create AuthProvider enum**

```java
package com.unibond.user.entity;

public enum AuthProvider {
    APPLE, EMAIL
}
```

- [ ] **Step 2: Create User entity**

```java
package com.unibond.user.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String nickname;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    private String appleSub;
    private Long partnerId;
    private String inviteCode;
    private String deviceToken;
    private String timezone;
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (timezone == null) timezone = "Asia/Shanghai";
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
    public String getAppleSub() { return appleSub; }
    public void setAppleSub(String appleSub) { this.appleSub = appleSub; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: Create UserRepository**

```java
package com.unibond.user.repository;

import com.unibond.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAppleSub(String appleSub);
    Optional<User> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
}
```

- [ ] **Step 4: Commit**

```bash
git add unibond-server/src/main/java/com/unibond/user/
git commit -m "feat: add User entity and repository"
```

---

### Task 5: Couple Entity + Service + Bind/Unbind

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/couple/entity/CoupleStatus.java`
- Create: `unibond-server/src/main/java/com/unibond/couple/entity/Couple.java`
- Create: `unibond-server/src/main/java/com/unibond/couple/repository/CoupleRepository.java`
- Create: `unibond-server/src/main/java/com/unibond/couple/dto/BindRequest.java`
- Create: `unibond-server/src/main/java/com/unibond/couple/dto/CoupleResponse.java`
- Create: `unibond-server/src/main/java/com/unibond/couple/service/CoupleService.java`
- Create: `unibond-server/src/main/java/com/unibond/couple/controller/CoupleController.java`
- Test: `unibond-server/src/test/java/com/unibond/couple/service/CoupleServiceTest.java`

- [ ] **Step 1: Write CoupleService test**

```java
package com.unibond.couple.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoupleServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private CoupleRepository coupleRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @InjectMocks private CoupleService coupleService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);
        userA.setInviteCode("ABC123");

        userB = new User();
        userB.setId(2L);
        userB.setInviteCode("XYZ789");
    }

    @Test
    void bind_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
        when(userRepository.findByInviteCode("XYZ789")).thenReturn(Optional.of(userB));
        when(coupleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Couple couple = coupleService.bind(1L, "XYZ789");

        assertEquals(1L, couple.getUserAId());
        assertEquals(2L, couple.getUserBId());
        assertEquals(CoupleStatus.ACTIVE, couple.getStatus());
        assertEquals(2L, userA.getPartnerId());
        assertEquals(1L, userB.getPartnerId());
    }

    @Test
    void bind_selfInviteCode_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userA));
        when(userRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(userA));

        BizException ex = assertThrows(BizException.class,
            () -> coupleService.bind(1L, "ABC123"));
        assertEquals(ErrorCode.INVITE_CODE_SELF, ex.getErrorCode());
    }

    @Test
    void bind_alreadyBound_throws() {
        userA.setPartnerId(3L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(userA));

        BizException ex = assertThrows(BizException.class,
            () -> coupleService.bind(1L, "XYZ789"));
        assertEquals(ErrorCode.COUPLE_ALREADY_BOUND, ex.getErrorCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd unibond-server && ./mvnw test -Dtest=CoupleServiceTest`
Expected: FAIL — CoupleService not found

- [ ] **Step 3: Implement entity, repository, DTO, service, controller**

```java
// CoupleStatus.java
package com.unibond.couple.entity;
public enum CoupleStatus { ACTIVE, DISSOLVED }
```

```java
// Couple.java
package com.unibond.couple.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "couples")
public class Couple {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userAId;
    private Long userBId;
    private LocalDate anniversaryDate;
    private Instant bindAt;

    @Enumerated(EnumType.STRING)
    private CoupleStatus status;

    @PrePersist
    void prePersist() {
        if (bindAt == null) bindAt = Instant.now();
        if (status == null) status = CoupleStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserAId() { return userAId; }
    public void setUserAId(Long userAId) { this.userAId = userAId; }
    public Long getUserBId() { return userBId; }
    public void setUserBId(Long userBId) { this.userBId = userBId; }
    public LocalDate getAnniversaryDate() { return anniversaryDate; }
    public void setAnniversaryDate(LocalDate d) { this.anniversaryDate = d; }
    public Instant getBindAt() { return bindAt; }
    public void setBindAt(Instant bindAt) { this.bindAt = bindAt; }
    public CoupleStatus getStatus() { return status; }
    public void setStatus(CoupleStatus status) { this.status = status; }
}
```

```java
// CoupleRepository.java
package com.unibond.couple.repository;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
    @Query("SELECT c FROM Couple c WHERE (c.userAId = :userId OR c.userBId = :userId) AND c.status = :status")
    Optional<Couple> findActiveByUserId(Long userId, CoupleStatus status);

    default Optional<Couple> findActiveByUserId(Long userId) {
        return findActiveByUserId(userId, CoupleStatus.ACTIVE);
    }

    List<Couple> findByStatus(CoupleStatus status);
}
```

```java
// BindRequest.java
package com.unibond.couple.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record BindRequest(@NotBlank @Size(min = 6, max = 6) String inviteCode) {}
```

```java
// CoupleResponse.java
package com.unibond.couple.dto;
import java.time.Instant;
import java.time.LocalDate;
public record CoupleResponse(Long id, Long partnerUserId, String partnerNickname,
    LocalDate anniversaryDate, Instant bindAt) {}
```

```java
// CoupleService.java
package com.unibond.couple.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoupleService {
    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;
    private final StringRedisTemplate redisTemplate;

    public CoupleService(UserRepository userRepository, CoupleRepository coupleRepository,
                         StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.coupleRepository = coupleRepository;
        this.redisTemplate = redisTemplate;
    }

    public Couple getActiveCouple(Long userId) {
        return coupleRepository.findActiveByUserId(userId)
            .orElseThrow(() -> new BizException(ErrorCode.COUPLE_NOT_BOUND));
    }

    @Transactional
    public Couple bind(Long userId, String inviteCode) {
        User me = userRepository.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        if (me.getPartnerId() != null) {
            throw new BizException(ErrorCode.COUPLE_ALREADY_BOUND);
        }

        User partner = userRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new BizException(ErrorCode.INVITE_CODE_INVALID));

        if (partner.getId().equals(userId)) {
            throw new BizException(ErrorCode.INVITE_CODE_SELF);
        }
        if (partner.getPartnerId() != null) {
            throw new BizException(ErrorCode.COUPLE_ALREADY_BOUND);
        }

        Couple couple = new Couple();
        couple.setUserAId(userId);
        couple.setUserBId(partner.getId());
        couple = coupleRepository.save(couple);

        me.setPartnerId(partner.getId());
        partner.setPartnerId(me.getId());
        userRepository.save(me);
        userRepository.save(partner);

        return couple;
    }

    @Transactional
    public void unbind(Long userId) {
        User me = userRepository.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (me.getPartnerId() == null) {
            throw new BizException(ErrorCode.COUPLE_NOT_BOUND);
        }

        User partner = userRepository.findById(me.getPartnerId())
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        // 1. Dissolve couple
        coupleRepository.findActiveByUserId(userId)
            .ifPresent(c -> {
                c.setStatus(CoupleStatus.DISSOLVED);
                coupleRepository.save(c);
            });

        // 2. Clear partner IDs
        me.setPartnerId(null);
        partner.setPartnerId(null);
        userRepository.save(me);
        userRepository.save(partner);

        // 3. Revoke refresh tokens for both users
        redisTemplate.delete("refresh:" + me.getId());
        redisTemplate.delete("refresh:" + partner.getId());

        // 4. Cancel today's DailyQuiz + send APNs end event + push notification
        //    are handled by the caller (controller) via PushService
    }
}
```

```java
// CoupleController.java
package com.unibond.couple.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.security.UserPrincipal;
import com.unibond.couple.dto.BindRequest;
import com.unibond.couple.dto.CoupleResponse;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.service.CoupleService;
import com.unibond.push.service.PushService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/couple")
public class CoupleController {
    private final CoupleService coupleService;
    private final UserRepository userRepository;
    private final PushService pushService;

    public CoupleController(CoupleService coupleService, UserRepository userRepository,
                            PushService pushService) {
        this.coupleService = coupleService;
        this.userRepository = userRepository;
        this.pushService = pushService;
    }

    @GetMapping("/info")
    public ApiResponse<CoupleResponse> info(@AuthenticationPrincipal UserPrincipal principal) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        Long partnerId = couple.getUserAId().equals(principal.userId())
            ? couple.getUserBId() : couple.getUserAId();
        User partner = userRepository.findById(partnerId).orElseThrow();
        return ApiResponse.ok(new CoupleResponse(
            couple.getId(), partnerId, partner.getNickname(),
            couple.getAnniversaryDate(), couple.getBindAt()));
    }

    @PostMapping("/bind")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CoupleResponse> bind(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BindRequest req) {
        Couple couple = coupleService.bind(principal.userId(), req.inviteCode());
        Long partnerId = couple.getUserAId().equals(principal.userId())
            ? couple.getUserBId() : couple.getUserAId();
        User partner = userRepository.findById(partnerId).orElseThrow();
        return ApiResponse.ok(new CoupleResponse(
            couple.getId(), partnerId, partner.getNickname(),
            couple.getAnniversaryDate(), couple.getBindAt()));
    }

    @DeleteMapping("/unbind")
    public ApiResponse<Void> unbind(@AuthenticationPrincipal UserPrincipal principal) {
        User me = userRepository.findById(principal.userId()).orElseThrow();
        User partner = userRepository.findById(me.getPartnerId()).orElse(null);

        coupleService.unbind(principal.userId());

        // Send push notification to partner
        if (partner != null && partner.getDeviceToken() != null) {
            pushService.sendPush(partner.getDeviceToken(),
                "UniBond", "你的情侣关系已被解除");
        }
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd unibond-server && ./mvnw test -Dtest=CoupleServiceTest`
Expected: PASS — all 3 tests green

- [ ] **Step 5: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add Couple bind/unbind with validation"
```

---

### Task 6: Auth Module (Email + Apple Login)

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/auth/dto/EmailSendRequest.java`
- Create: `unibond-server/src/main/java/com/unibond/auth/dto/EmailLoginRequest.java`
- Create: `unibond-server/src/main/java/com/unibond/auth/dto/AppleLoginRequest.java`
- Create: `unibond-server/src/main/java/com/unibond/auth/dto/AuthResponse.java`
- Create: `unibond-server/src/main/java/com/unibond/auth/service/AuthService.java`
- Create: `unibond-server/src/main/java/com/unibond/auth/service/EmailService.java`
- Create: `unibond-server/src/main/java/com/unibond/auth/service/AppleAuthService.java`
- Create: `unibond-server/src/main/java/com/unibond/auth/controller/AuthController.java`
- Create: `unibond-server/src/main/resources/templates/verification-code.html`
- Test: `unibond-server/src/test/java/com/unibond/auth/service/AuthServiceTest.java`

- [ ] **Step 1: Write AuthService test**

```java
package com.unibond.auth.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.common.security.JwtProvider;
import com.unibond.user.entity.AuthProvider;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtProvider, redisTemplate,
            "ABCDEFGHJKMNPQRSTUVWXYZ23456789", 6, 3);
    }

    @Test
    void emailLogin_newUser_createsUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("email_code:test@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtProvider.createAccessToken(1L)).thenReturn("access");
        when(jwtProvider.createRefreshToken(1L)).thenReturn("refresh");
        when(redisTemplate.delete("email_code:test@test.com")).thenReturn(true);

        var response = authService.emailLogin("test@test.com", "123456");

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        verify(userRepository).save(any());
    }

    @Test
    void emailLogin_wrongCode_throws() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("email_code:test@test.com")).thenReturn("999999");

        BizException ex = assertThrows(BizException.class,
            () -> authService.emailLogin("test@test.com", "123456"));
        assertEquals(ErrorCode.AUTH_CODE_INVALID, ex.getErrorCode());
    }

    @Test
    void emailLogin_expiredCode_throws() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("email_code:test@test.com")).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
            () -> authService.emailLogin("test@test.com", "123456"));
        assertEquals(ErrorCode.AUTH_CODE_EXPIRED, ex.getErrorCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd unibond-server && ./mvnw test -Dtest=AuthServiceTest`
Expected: FAIL — AuthService not found

- [ ] **Step 3: Implement DTOs**

```java
// EmailSendRequest.java
package com.unibond.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record EmailSendRequest(@NotBlank @Email String email) {}

// EmailLoginRequest.java
package com.unibond.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record EmailLoginRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 6) String code,
    String timezone) {}

// AppleLoginRequest.java
package com.unibond.auth.dto;
import jakarta.validation.constraints.NotBlank;
public record AppleLoginRequest(
    @NotBlank String identityToken,
    String nickname,
    String timezone) {}

// AuthResponse.java
package com.unibond.auth.dto;
public record AuthResponse(String accessToken, String refreshToken, Long userId, boolean isNew) {}
```

- [ ] **Step 4: Implement AuthService**

```java
package com.unibond.auth.service;

import com.unibond.auth.dto.AuthResponse;
import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.common.security.JwtProvider;
import com.unibond.user.entity.AuthProvider;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final String inviteCharset;
    private final int inviteLength;
    private final int inviteMaxRetries;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository, JwtProvider jwtProvider,
                       StringRedisTemplate redisTemplate,
                       @Value("${app.invite-code.charset}") String inviteCharset,
                       @Value("${app.invite-code.length}") int inviteLength,
                       @Value("${app.invite-code.max-retries}") int inviteMaxRetries) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.inviteCharset = inviteCharset;
        this.inviteLength = inviteLength;
        this.inviteMaxRetries = inviteMaxRetries;
    }

    @Transactional
    public AuthResponse emailLogin(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get("email_code:" + email);
        if (storedCode == null) throw new BizException(ErrorCode.AUTH_CODE_EXPIRED);
        if (!storedCode.equals(code)) throw new BizException(ErrorCode.AUTH_CODE_INVALID);

        boolean isNew = false;
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setAuthProvider(AuthProvider.EMAIL);
            user.setInviteCode(generateInviteCode());
            user = userRepository.save(user);
            isNew = true;
        }

        redisTemplate.delete("email_code:" + email);
        return issueTokens(user, isNew);
    }

    @Transactional
    public AuthResponse appleLogin(String appleSub, String nickname, String timezone) {
        boolean isNew = false;
        User user = userRepository.findByAppleSub(appleSub).orElse(null);
        if (user == null) {
            user = new User();
            user.setAppleSub(appleSub);
            user.setAuthProvider(AuthProvider.APPLE);
            user.setNickname(nickname);
            user.setTimezone(timezone);
            user.setInviteCode(generateInviteCode());
            user = userRepository.save(user);
            isNew = true;
        }
        return issueTokens(user, isNew);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtProvider.validate(refreshToken)) {
            throw new BizException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new BizException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        return issueTokens(user, false);
    }

    public void logout(Long userId) {
        redisTemplate.delete("refresh:" + userId);
    }

    public void logoutByRefreshToken(String refreshToken) {
        if (refreshToken != null && jwtProvider.validate(refreshToken)) {
            Long userId = jwtProvider.getUserId(refreshToken);
            redisTemplate.delete("refresh:" + userId);
        }
    }

    private AuthResponse issueTokens(User user, boolean isNew) {
        String access = jwtProvider.createAccessToken(user.getId());
        String refresh = jwtProvider.createRefreshToken(user.getId());
        // Token rotation: overwrite old refresh token in Redis
        redisTemplate.opsForValue().set("refresh:" + user.getId(), refresh, 30, TimeUnit.DAYS);
        return new AuthResponse(access, refresh, user.getId(), isNew);
    }

    private String generateInviteCode() {
        for (int i = 0; i < inviteMaxRetries; i++) {
            StringBuilder sb = new StringBuilder(inviteLength);
            for (int j = 0; j < inviteLength; j++) {
                sb.append(inviteCharset.charAt(random.nextInt(inviteCharset.length())));
            }
            String code = sb.toString();
            if (!userRepository.existsByInviteCode(code)) return code;
        }
        throw new RuntimeException("Failed to generate unique invite code");
    }

    public void sendEmailCode(String email) {
        // Rate limit: 1 per 60s per email
        String rateLimitKey = "email_rate:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BizException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        String code = String.format("%06d", random.nextInt(1000000));
        redisTemplate.opsForValue().set("email_code:" + email, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);
    }

    public String getEmailCode(String email) {
        // Used by EmailService to get the generated code for sending
        return redisTemplate.opsForValue().get("email_code:" + email);
    }
}
```

- [ ] **Step 5: Implement EmailService**

```java
package com.unibond.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                        @Value("${spring.mail.username:noreply@unibond.app}") String fromEmail) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
    }

    public void sendVerificationCode(String to, String code) throws MessagingException {
        Context ctx = new Context();
        ctx.setVariable("code", code);
        String html = templateEngine.process("verification-code", ctx);

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("UniBond 验证码");
        helper.setText(html, true);
        mailSender.send(msg);
    }
}
```

- [ ] **Step 5b: Implement AppleAuthService**

```java
package com.unibond.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class AppleAuthService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extract Apple 'sub' from identity token JWT.
     * MVP: decode payload without full signature verification.
     * Production: validate against Apple's public keys (https://appleid.apple.com/auth/keys).
     */
    public String extractAppleSub(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT format");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode node = objectMapper.readTree(payload);
            String sub = node.get("sub").asText();
            if (sub == null || sub.isBlank()) throw new IllegalArgumentException("Missing sub claim");
            return sub;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Apple identity token", e);
        }
    }
}
```

- [ ] **Step 6: Create email template**

```html
<!-- src/main/resources/templates/verification-code.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body style="font-family: sans-serif; padding: 20px;">
    <h2>UniBond 验证码</h2>
    <p>您的验证码是：</p>
    <h1 style="color: #E91E63; letter-spacing: 8px;" th:text="${code}">000000</h1>
    <p>验证码有效期为 5 分钟，请尽快使用。</p>
</body>
</html>
```

- [ ] **Step 7: Implement AuthController**

```java
package com.unibond.auth.controller;

import com.unibond.auth.dto.*;
import com.unibond.auth.service.AppleAuthService;
import com.unibond.auth.service.AuthService;
import com.unibond.auth.service.EmailService;
import com.unibond.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final EmailService emailService;
    private final AppleAuthService appleAuthService;

    public AuthController(AuthService authService, EmailService emailService,
                          AppleAuthService appleAuthService) {
        this.authService = authService;
        this.emailService = emailService;
        this.appleAuthService = appleAuthService;
    }

    @PostMapping("/email/send")
    public ApiResponse<Void> sendCode(@Valid @RequestBody EmailSendRequest req) throws Exception {
        authService.sendEmailCode(req.email());
        String code = authService.getEmailCode(req.email());
        emailService.sendVerificationCode(req.email(), code);
        return ApiResponse.ok(null);
    }

    @PostMapping("/email/login")
    public ApiResponse<AuthResponse> emailLogin(@Valid @RequestBody EmailLoginRequest req) {
        return ApiResponse.ok(authService.emailLogin(req.email(), req.code()));
    }

    @PostMapping("/apple")
    public ApiResponse<AuthResponse> appleLogin(@Valid @RequestBody AppleLoginRequest req) {
        String appleSub = appleAuthService.extractAppleSub(req.identityToken());
        return ApiResponse.ok(authService.appleLogin(appleSub, req.nickname(), req.timezone()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody java.util.Map<String, String> body) {
        return ApiResponse.ok(authService.refresh(body.get("refreshToken")));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody java.util.Map<String, String> body) {
        // Accepts refreshToken in body — access token may be expired
        authService.logoutByRefreshToken(body.get("refreshToken"));
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

Run: `cd unibond-server && ./mvnw test -Dtest=AuthServiceTest`
Expected: PASS — all 3 tests green

- [ ] **Step 9: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add Auth module (email verification + Apple login + JWT)"
```

---

### Task 7: User Module (Profile + Device Token)

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/user/dto/ProfileUpdateRequest.java`
- Create: `unibond-server/src/main/java/com/unibond/user/dto/UserResponse.java`
- Create: `unibond-server/src/main/java/com/unibond/user/service/UserService.java`
- Create: `unibond-server/src/main/java/com/unibond/user/controller/UserController.java`

- [ ] **Step 1: Implement DTOs and service**

```java
// ProfileUpdateRequest.java
package com.unibond.user.dto;
import jakarta.validation.constraints.Size;
public record ProfileUpdateRequest(
    @Size(max = 20) String nickname,
    @Size(max = 500) String avatarUrl) {}

// UserResponse.java
package com.unibond.user.dto;
import com.unibond.user.entity.AuthProvider;
import java.time.Instant;
public record UserResponse(Long id, String email, String nickname, String avatarUrl,
    AuthProvider authProvider, String inviteCode, Long partnerId, Instant createdAt) {}
```

```java
// UserService.java
package com.unibond.user.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.common.util.InputSanitizer;
import com.unibond.couple.service.CoupleService;
import com.unibond.user.dto.UserResponse;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CoupleService coupleService;
    private final StringRedisTemplate redisTemplate;

    public UserService(UserRepository userRepository, CoupleService coupleService,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.coupleService = coupleService;
        this.redisTemplate = redisTemplate;
    }

    public UserResponse getMe(Long userId) {
        User u = findUser(userId);
        return toResponse(u);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, String nickname, String avatarUrl) {
        User u = findUser(userId);
        if (nickname != null) u.setNickname(InputSanitizer.sanitizeText(nickname, 20));
        if (avatarUrl != null) u.setAvatarUrl(avatarUrl);
        return toResponse(userRepository.save(u));
    }

    @Transactional
    public void updateDeviceToken(Long userId, String deviceToken) {
        User u = findUser(userId);
        u.setDeviceToken(deviceToken);
        userRepository.save(u);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUser(userId);

        // 1. Unbind couple if bound (handles partner cleanup)
        if (user.getPartnerId() != null) {
            coupleService.unbind(userId);
        }

        // 2. Revoke refresh token
        redisTemplate.delete("refresh:" + userId);

        // 3. Nullify FK references in quiz_answers, mood_status (set user_id references)
        //    Note: historical data preserved per spec, but user record removed
        //    DB schema should use ON DELETE SET NULL for user FK references
        userRepository.deleteById(userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getNickname(), u.getAvatarUrl(),
            u.getAuthProvider(), u.getInviteCode(), u.getPartnerId(), u.getCreatedAt());
    }
}
```

- [ ] **Step 2: Implement UserController**

```java
package com.unibond.user.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.security.UserPrincipal;
import com.unibond.user.dto.ProfileUpdateRequest;
import com.unibond.user.dto.UserResponse;
import com.unibond.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getMe(principal.userId()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest req) {
        return ApiResponse.ok(userService.updateProfile(principal.userId(), req.nickname(), req.avatarUrl()));
    }

    @PostMapping("/device-token")
    public ApiResponse<Void> updateDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody java.util.Map<String, String> body) {
        userService.updateDeviceToken(principal.userId(), body.get("deviceToken"));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteAccount(principal.userId());
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add unibond-server/src/main/java/com/unibond/user/
git commit -m "feat: add User profile and device token management"
```

---

### Task 8: Quiz Entities + Core Quiz Service

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/quiz/entity/QuizType.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/entity/GenerationSource.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/entity/DailyQuiz.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/entity/QuizAnswer.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/entity/QuestionPool.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/repository/DailyQuizRepository.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/repository/QuizAnswerRepository.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/repository/QuestionPoolRepository.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/dto/QuizResponse.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/dto/AnswerRequest.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/dto/QuizResultResponse.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/service/QuizService.java`
- Create: `unibond-server/src/main/java/com/unibond/quiz/controller/QuizController.java`
- Test: `unibond-server/src/test/java/com/unibond/quiz/service/QuizServiceTest.java`

- [ ] **Step 1: Write QuizService test for answer submission and reveal**

```java
package com.unibond.quiz.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.quiz.entity.*;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuizAnswerRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock private DailyQuizRepository quizRepo;
    @Mock private QuizAnswerRepository answerRepo;
    @Mock private UserRepository userRepo;

    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(quizRepo, answerRepo, userRepo);
    }

    @Test
    void submitAnswer_idempotent_returnsExisting() {
        QuizAnswer existing = new QuizAnswer();
        existing.setId(1L);
        when(answerRepo.findByDailyQuizIdAndUserId(10L, 1L))
            .thenReturn(Optional.of(existing));

        QuizAnswer result = quizService.submitAnswer(1L, 10L, 5L, "[1,2,3,4,5]", null);
        assertEquals(1L, result.getId());
        verify(answerRepo, never()).save(any());
    }

    @Test
    void submitAnswer_secondSubmission_triggersReveal() {
        when(answerRepo.findByDailyQuizIdAndUserId(10L, 2L))
            .thenReturn(Optional.empty());

        QuizAnswer firstAnswer = new QuizAnswer();
        firstAnswer.setId(1L);
        firstAnswer.setUserId(1L);
        firstAnswer.setAnswers("[\"A\",\"B\",\"C\",\"D\",\"A\"]");
        firstAnswer.setRevealed(false);

        when(answerRepo.countByDailyQuizIdForUpdate(10L)).thenReturn(1L);
        when(answerRepo.findByDailyQuizId(10L)).thenReturn(List.of(firstAnswer));
        when(answerRepo.save(any())).thenAnswer(inv -> {
            QuizAnswer a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        QuizAnswer result = quizService.submitAnswer(2L, 10L, 5L,
            "[\"A\",\"B\",\"C\",\"D\",\"A\"]", null);

        assertTrue(result.getRevealed());
        assertTrue(firstAnswer.getRevealed());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd unibond-server && ./mvnw test -Dtest=QuizServiceTest`
Expected: FAIL — QuizService not found

- [ ] **Step 3: Implement entities and enums**

```java
// QuizType.java
package com.unibond.quiz.entity;
public enum QuizType { BLIND, GUESS, THEME }

// GenerationSource.java
package com.unibond.quiz.entity;
public enum GenerationSource { AI, FALLBACK_POOL }
```

```java
// DailyQuiz.java
package com.unibond.quiz.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_quizzes")
public class DailyQuiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_date")
    private LocalDate date;
    private Long coupleId;

    @Enumerated(EnumType.STRING)
    private QuizType quizType;

    private String theme;

    @JdbcTypeCode(SqlTypes.JSON)
    private String questions;

    @Enumerated(EnumType.STRING)
    private GenerationSource generationSource;

    @JdbcTypeCode(SqlTypes.JSON)
    private String promptContext;

    private Instant createdAt;

    @PrePersist void prePersist() { if (createdAt == null) createdAt = Instant.now(); }

    // Getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long coupleId) { this.coupleId = coupleId; }
    public QuizType getQuizType() { return quizType; }
    public void setQuizType(QuizType quizType) { this.quizType = quizType; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getQuestions() { return questions; }
    public void setQuestions(String questions) { this.questions = questions; }
    public GenerationSource getGenerationSource() { return generationSource; }
    public void setGenerationSource(GenerationSource s) { this.generationSource = s; }
    public String getPromptContext() { return promptContext; }
    public void setPromptContext(String p) { this.promptContext = p; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

```java
// QuizAnswer.java
package com.unibond.quiz.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "quiz_answers")
public class QuizAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long dailyQuizId;
    private Long userId;
    private Long coupleId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String answers;

    @JdbcTypeCode(SqlTypes.JSON)
    private String partnerGuess;

    private Integer score;
    private Instant completedAt;
    private Boolean revealed;

    @PrePersist void prePersist() {
        if (completedAt == null) completedAt = Instant.now();
        if (revealed == null) revealed = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDailyQuizId() { return dailyQuizId; }
    public void setDailyQuizId(Long dailyQuizId) { this.dailyQuizId = dailyQuizId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long coupleId) { this.coupleId = coupleId; }
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    public String getPartnerGuess() { return partnerGuess; }
    public void setPartnerGuess(String partnerGuess) { this.partnerGuess = partnerGuess; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Boolean getRevealed() { return revealed; }
    public void setRevealed(Boolean revealed) { this.revealed = revealed; }
}
```

```java
// QuestionPool.java
package com.unibond.quiz.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "question_pool")
public class QuestionPool {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    @Enumerated(EnumType.STRING)
    private QuizType quizType;
    private String question;
    @JdbcTypeCode(SqlTypes.JSON)
    private String options;
    private int difficulty;
    private int usedCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }
    public QuizType getQuizType() { return quizType; }
    public void setQuizType(QuizType t) { this.quizType = t; }
    public String getQuestion() { return question; }
    public void setQuestion(String q) { this.question = q; }
    public String getOptions() { return options; }
    public void setOptions(String o) { this.options = o; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int d) { this.difficulty = d; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int c) { this.usedCount = c; }
}
```

- [ ] **Step 4: Implement repositories**

```java
// DailyQuizRepository.java
package com.unibond.quiz.repository;
import com.unibond.quiz.entity.DailyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface DailyQuizRepository extends JpaRepository<DailyQuiz, Long> {
    Optional<DailyQuiz> findByCoupleIdAndDate(Long coupleId, LocalDate date);
    List<DailyQuiz> findByCoupleIdAndIdLessThanOrderByIdDesc(Long coupleId, Long cursor, org.springframework.data.domain.Pageable pageable);
    List<DailyQuiz> findByCoupleIdOrderByIdDesc(Long coupleId, org.springframework.data.domain.Pageable pageable);
}

// QuizAnswerRepository.java
package com.unibond.quiz.repository;
import com.unibond.quiz.entity.QuizAnswer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    Optional<QuizAnswer> findByDailyQuizIdAndUserId(Long dailyQuizId, Long userId);
    List<QuizAnswer> findByDailyQuizId(Long dailyQuizId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(a) FROM QuizAnswer a WHERE a.dailyQuizId = :quizId")
    long countByDailyQuizIdForUpdate(Long quizId);
}

// QuestionPoolRepository.java
package com.unibond.quiz.repository;
import com.unibond.quiz.entity.QuestionPool;
import com.unibond.quiz.entity.QuizType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface QuestionPoolRepository extends JpaRepository<QuestionPool, Long> {
    @Query("SELECT q FROM QuestionPool q WHERE q.quizType = :type ORDER BY q.usedCount ASC, FUNCTION('RANDOM') LIMIT 5")
    List<QuestionPool> findLeastUsedByType(QuizType type);
}
```

- [ ] **Step 5: Implement QuizService with idempotent answer + reveal logic**

```java
package com.unibond.quiz.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.quiz.entity.QuizAnswer;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuizAnswerRepository;
import com.unibond.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class QuizService {
    private final DailyQuizRepository quizRepo;
    private final QuizAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(DailyQuizRepository quizRepo, QuizAnswerRepository answerRepo,
                       UserRepository userRepo) {
        this.quizRepo = quizRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public QuizAnswer submitAnswer(Long userId, Long quizId, Long coupleId,
                                    String answers, String partnerGuess) {
        // Idempotent: return existing if already answered
        var existing = answerRepo.findByDailyQuizIdAndUserId(quizId, userId);
        if (existing.isPresent()) return existing.get();

        QuizAnswer answer = new QuizAnswer();
        answer.setDailyQuizId(quizId);
        answer.setUserId(userId);
        answer.setCoupleId(coupleId);
        answer.setAnswers(answers);
        answer.setPartnerGuess(partnerGuess);

        // Check if both answered → reveal
        long count = answerRepo.countByDailyQuizIdForUpdate(quizId);
        if (count >= 1) {
            // This is the second answer — calculate scores and reveal
            List<QuizAnswer> allAnswers = answerRepo.findByDailyQuizId(quizId);
            QuizAnswer first = allAnswers.get(0);

            int score = calculateScore(first.getAnswers(), answers);
            answer.setScore(score);
            first.setScore(score);
            answer.setRevealed(true);
            first.setRevealed(true);
            answerRepo.save(first);
        } else {
            answer.setRevealed(false);
        }

        return answerRepo.save(answer);
    }

    private int calculateScore(String answersA, String answersB) {
        try {
            List<String> a = objectMapper.readValue(answersA, new TypeReference<>() {});
            List<String> b = objectMapper.readValue(answersB, new TypeReference<>() {});
            int matches = 0;
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                if (a.get(i).equals(b.get(i))) matches++;
            }
            return matches * 100 / 5;
        } catch (Exception e) {
            return 0;
        }
    }
}
```

- [ ] **Step 6: Implement DTOs and controller**

```java
// QuizResponse.java
package com.unibond.quiz.dto;
import com.unibond.quiz.entity.QuizType;
import java.time.LocalDate;
public record QuizResponse(Long id, LocalDate date, QuizType quizType,
    String theme, String questions) {}

// AnswerRequest.java
package com.unibond.quiz.dto;
import jakarta.validation.constraints.NotNull;
public record AnswerRequest(@NotNull Long quizId, @NotNull String answers, String partnerGuess) {}

// QuizResultResponse.java
package com.unibond.quiz.dto;
import java.util.List;
public record QuizResultResponse(int score, boolean revealed,
    String myAnswers, String partnerAnswers, String questions) {}
```

```java
// QuizController.java
package com.unibond.quiz.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.dto.CursorPage;
import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.common.security.UserPrincipal;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.service.CoupleService;
import com.unibond.quiz.dto.*;
import com.unibond.quiz.entity.DailyQuiz;
import com.unibond.quiz.entity.QuizAnswer;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuizAnswerRepository;
import com.unibond.quiz.service.QuizService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz")
public class QuizController {
    private final QuizService quizService;
    private final DailyQuizRepository quizRepo;
    private final QuizAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final CoupleService coupleService;

    public QuizController(QuizService quizService, DailyQuizRepository quizRepo,
                          QuizAnswerRepository answerRepo, UserRepository userRepo,
                          CoupleService coupleService) {
        this.quizService = quizService;
        this.quizRepo = quizRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
        this.coupleService = coupleService;
    }

    @GetMapping("/today")
    public ApiResponse<QuizResponse> today(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepo.findById(principal.userId()).orElseThrow();
        Couple couple = coupleService.getActiveCouple(principal.userId());
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

        DailyQuiz quiz = quizRepo.findByCoupleIdAndDate(couple.getId(), today)
            .orElseThrow(() -> new BizException(ErrorCode.QUIZ_NOT_AVAILABLE));

        return ApiResponse.ok(new QuizResponse(quiz.getId(), quiz.getDate(),
            quiz.getQuizType(), quiz.getTheme(), quiz.getQuestions()));
    }

    @PostMapping("/answer")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizResultResponse> answer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AnswerRequest req) {
        Couple couple = coupleService.getActiveCouple(principal.userId());

        QuizAnswer answer = quizService.submitAnswer(
            principal.userId(), req.quizId(), couple.getId(), req.answers(), req.partnerGuess());

        String partnerAnswers = null;
        if (answer.getRevealed()) {
            partnerAnswers = answerRepo.findByDailyQuizId(req.quizId()).stream()
                .filter(a -> !a.getUserId().equals(principal.userId()))
                .map(QuizAnswer::getAnswers)
                .findFirst().orElse(null);
        }

        DailyQuiz quiz = quizRepo.findById(req.quizId()).orElseThrow();
        return ApiResponse.ok(new QuizResultResponse(
            answer.getScore() != null ? answer.getScore() : 0,
            answer.getRevealed(),
            answer.getAnswers(), partnerAnswers, quiz.getQuestions()));
    }

    @GetMapping("/result/{date}")
    public ApiResponse<QuizResultResponse> result(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String date) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        LocalDate d = LocalDate.parse(date);

        DailyQuiz quiz = quizRepo.findByCoupleIdAndDate(couple.getId(), d)
            .orElseThrow(() -> new BizException(ErrorCode.QUIZ_NOT_AVAILABLE));

        QuizAnswer myAnswer = answerRepo.findByDailyQuizIdAndUserId(quiz.getId(), principal.userId())
            .orElseThrow(() -> new BizException(ErrorCode.QUIZ_NOT_AVAILABLE));

        if (!myAnswer.getRevealed()) throw new BizException(ErrorCode.QUIZ_NOT_REVEALED);

        String partnerAnswers = answerRepo.findByDailyQuizId(quiz.getId()).stream()
            .filter(a -> !a.getUserId().equals(principal.userId()))
            .map(QuizAnswer::getAnswers)
            .findFirst().orElse(null);

        return ApiResponse.ok(new QuizResultResponse(
            myAnswer.getScore(), true, myAnswer.getAnswers(), partnerAnswers, quiz.getQuestions()));
    }

    @GetMapping("/history")
    public ApiResponse<CursorPage<QuizResponse>> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        size = Math.min(size, 50);

        List<DailyQuiz> quizzes;
        if (cursor != null) {
            quizzes = quizRepo.findByCoupleIdAndIdLessThanOrderByIdDesc(
                couple.getId(), cursor, PageRequest.of(0, size + 1));
        } else {
            quizzes = quizRepo.findByCoupleIdOrderByIdDesc(
                couple.getId(), PageRequest.of(0, size + 1));
        }

        boolean hasMore = quizzes.size() > size;
        if (hasMore) quizzes = quizzes.subList(0, size);

        List<QuizResponse> data = quizzes.stream()
            .map(q -> new QuizResponse(q.getId(), q.getDate(), q.getQuizType(),
                q.getTheme(), q.getQuestions()))
            .toList();

        String nextCursor = hasMore ? quizzes.get(quizzes.size() - 1).getId().toString() : null;
        return ApiResponse.ok(new CursorPage<>(data, nextCursor, hasMore));
    }
}
```

- [ ] **Step 7: Run tests**

Run: `cd unibond-server && ./mvnw test -Dtest=QuizServiceTest`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add Quiz module with answer submission and reveal logic"
```

---

### Task 9: Quiz Generation Service (LLM + Fallback)

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/quiz/service/QuizGenerationService.java`
- Test: `unibond-server/src/test/java/com/unibond/quiz/service/QuizGenerationServiceTest.java`

- [ ] **Step 1: Write test for quiz generation with fallback**

```java
package com.unibond.quiz.service;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.quiz.entity.*;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuestionPoolRepository;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizGenerationServiceTest {
    @Mock private DailyQuizRepository quizRepo;
    @Mock private QuestionPoolRepository poolRepo;
    @Mock private CoupleRepository coupleRepo;
    @Mock private UserRepository userRepo;
    @InjectMocks private QuizGenerationService service;

    @Test
    void generateFallback_usesQuestionPool() {
        Couple couple = new Couple();
        couple.setId(1L);
        couple.setStatus(CoupleStatus.ACTIVE);
        when(coupleRepo.findByStatus(CoupleStatus.ACTIVE)).thenReturn(List.of(couple));
        when(quizRepo.findByCoupleIdAndDate(anyLong(), any())).thenReturn(java.util.Optional.empty());

        QuestionPool q = new QuestionPool();
        q.setQuestion("你最喜欢的颜色？");
        q.setOptions("[\"红\",\"蓝\",\"绿\",\"黄\"]");
        when(poolRepo.findLeastUsedByType(any())).thenReturn(List.of(q, q, q, q, q));
        when(quizRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.generateDailyQuizzesFromPool();

        verify(quizRepo).save(argThat(quiz ->
            quiz.getGenerationSource() == GenerationSource.FALLBACK_POOL));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd unibond-server && ./mvnw test -Dtest=QuizGenerationServiceTest`
Expected: FAIL

- [ ] **Step 3: Implement QuizGenerationService**

```java
package com.unibond.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.quiz.entity.*;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuestionPoolRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizGenerationService {
    private static final Logger log = LoggerFactory.getLogger(QuizGenerationService.class);
    private final DailyQuizRepository quizRepo;
    private final QuestionPoolRepository poolRepo;
    private final CoupleRepository coupleRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizGenerationService(DailyQuizRepository quizRepo,
                                  QuestionPoolRepository poolRepo,
                                  CoupleRepository coupleRepo,
                                  UserRepository userRepo) {
        this.quizRepo = quizRepo;
        this.poolRepo = poolRepo;
        this.coupleRepo = coupleRepo;
        this.userRepo = userRepo;
    }

    // Run every hour to cover all timezones (generates only for couples whose local time is 00:xx)
    @Scheduled(cron = "0 5 * * * *")
    public void scheduledGeneration() {
        // Find timezones where it's currently midnight (00:00-00:59)
        Set<String> midnightZones = ZoneId.getAvailableZoneIds().stream()
            .filter(tz -> {
                int hour = ZonedDateTime.now(ZoneId.of(tz)).getHour();
                return hour == 0;
            })
            .collect(Collectors.toSet());

        List<Couple> activeCouples = coupleRepo.findByStatus(CoupleStatus.ACTIVE);

        for (Couple couple : activeCouples) {
            User userA = userRepo.findById(couple.getUserAId()).orElse(null);
            if (userA == null) continue;
            String tz = userA.getTimezone() != null ? userA.getTimezone() : "Asia/Shanghai";
            if (!midnightZones.contains(tz)) continue;

            LocalDate today = LocalDate.now(ZoneId.of(tz));
            generateForCouple(couple, today);
        }
    }

    // Fallback: generate from question pool (also used in tests)
    public void generateDailyQuizzesFromPool() {
        LocalDate today = LocalDate.now();
        List<Couple> activeCouples = coupleRepo.findByStatus(CoupleStatus.ACTIVE);
        for (Couple couple : activeCouples) {
            generateForCouple(couple, today);
        }
    }

    private void generateForCouple(Couple couple, LocalDate today) {
        if (quizRepo.findByCoupleIdAndDate(couple.getId(), today).isPresent()) {
            return; // already generated
        }
        try {
            QuizType type = getQuizType(today);
            List<QuestionPool> questions = poolRepo.findLeastUsedByType(type);
            String questionsJson = objectMapper.writeValueAsString(
                questions.stream().map(q -> java.util.Map.of(
                    "question", q.getQuestion(),
                    "options", q.getOptions()
                )).collect(Collectors.toList())
            );

            DailyQuiz quiz = new DailyQuiz();
            quiz.setDate(today);
            quiz.setCoupleId(couple.getId());
            quiz.setQuizType(type);
            quiz.setQuestions(questionsJson);
            quiz.setGenerationSource(GenerationSource.FALLBACK_POOL);
            quizRepo.save(quiz);

            questions.forEach(q -> {
                q.setUsedCount(q.getUsedCount() + 1);
                poolRepo.save(q);
            });
        } catch (Exception e) {
            log.error("Failed to generate quiz for couple {}", couple.getId(), e);
        }
    }

    static QuizType getQuizType(LocalDate date) {
        int day = (int)(date.toEpochDay() % 3);
        return switch (day) {
            case 0 -> QuizType.BLIND;
            case 1 -> QuizType.GUESS;
            default -> QuizType.THEME;
        };
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd unibond-server && ./mvnw test -Dtest=QuizGenerationServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add QuizGenerationService with fallback pool"
```

---

### Task 10: Mood Module

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/mood/entity/MoodStatus.java`
- Create: `unibond-server/src/main/java/com/unibond/mood/repository/MoodRepository.java`
- Create: `unibond-server/src/main/java/com/unibond/mood/dto/MoodUpdateRequest.java`
- Create: `unibond-server/src/main/java/com/unibond/mood/dto/MoodResponse.java`
- Create: `unibond-server/src/main/java/com/unibond/mood/service/MoodService.java`
- Create: `unibond-server/src/main/java/com/unibond/mood/controller/MoodController.java`
- Test: `unibond-server/src/test/java/com/unibond/mood/service/MoodServiceTest.java`

- [ ] **Step 1: Write MoodService test**

```java
package com.unibond.mood.service;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.service.CoupleService;
import com.unibond.mood.entity.MoodStatus;
import com.unibond.mood.repository.MoodRepository;
import com.unibond.push.service.LiveActivityPushService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoodServiceTest {
    @Mock private MoodRepository moodRepo;
    @Mock private UserRepository userRepo;
    @Mock private CoupleService coupleService;
    @Mock private LiveActivityPushService liveActivityPush;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @InjectMocks private MoodService moodService;

    @Test
    void updateMood_savesToDbAndRedisAndPushes() {
        User user = new User();
        user.setId(1L);
        user.setPartnerId(2L);
        User partner = new User();
        partner.setId(2L);
        partner.setDeviceToken("token123");

        Couple couple = new Couple();
        couple.setId(10L);
        couple.setUserAId(1L);
        couple.setUserBId(2L);

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findById(2L)).thenReturn(Optional.of(partner));
        when(coupleService.getActiveCouple(1L)).thenReturn(couple);
        when(moodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        MoodStatus result = moodService.updateMood(1L, "😊", "今天真开心");

        assertEquals("😊", result.getMoodEmoji());
        assertEquals("今天真开心", result.getMoodText());
        verify(moodRepo).save(any());
        verify(valueOps).set(eq("mood:1"), anyString());
        verify(liveActivityPush).updateMoodActivity("token123", "😊", "今天真开心");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd unibond-server && ./mvnw test -Dtest=MoodServiceTest`
Expected: FAIL

- [ ] **Step 3: Implement mood module**

```java
// MoodStatus.java
package com.unibond.mood.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mood_status")
public class MoodStatus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long coupleId;
    private String moodEmoji;
    private String moodText;
    private Instant updatedAt;

    @PrePersist @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long coupleId) { this.coupleId = coupleId; }
    public String getMoodEmoji() { return moodEmoji; }
    public void setMoodEmoji(String e) { this.moodEmoji = e; }
    public String getMoodText() { return moodText; }
    public void setMoodText(String t) { this.moodText = t; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant u) { this.updatedAt = u; }
}

// MoodRepository.java
package com.unibond.mood.repository;
import com.unibond.mood.entity.MoodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MoodRepository extends JpaRepository<MoodStatus, Long> {
    Optional<MoodStatus> findTopByUserIdOrderByUpdatedAtDesc(Long userId);
}

// MoodUpdateRequest.java
package com.unibond.mood.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record MoodUpdateRequest(
    @NotBlank @Size(max = 10) String emoji,
    @Size(max = 50) String text) {}

// MoodResponse.java
package com.unibond.mood.dto;
import java.time.Instant;
public record MoodResponse(String emoji, String text, Instant updatedAt) {}
```

```java
// MoodService.java
package com.unibond.mood.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.service.CoupleService;
import com.unibond.mood.entity.MoodStatus;
import com.unibond.mood.repository.MoodRepository;
import com.unibond.push.service.LiveActivityPushService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class MoodService {
    private final MoodRepository moodRepo;
    private final UserRepository userRepo;
    private final CoupleService coupleService;
    private final LiveActivityPushService liveActivityPush;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MoodService(MoodRepository moodRepo, UserRepository userRepo,
                       CoupleService coupleService, LiveActivityPushService liveActivityPush,
                       StringRedisTemplate redisTemplate) {
        this.moodRepo = moodRepo;
        this.userRepo = userRepo;
        this.coupleService = coupleService;
        this.liveActivityPush = liveActivityPush;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public MoodStatus updateMood(Long userId, String emoji, String text) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        Couple couple = coupleService.getActiveCouple(userId);

        MoodStatus mood = new MoodStatus();
        mood.setUserId(userId);
        mood.setCoupleId(couple.getId());
        mood.setMoodEmoji(emoji);
        mood.setMoodText(text);
        mood = moodRepo.save(mood);

        // Cache in Redis
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                "emoji", emoji,
                "text", text != null ? text : "",
                "updatedAt", mood.getUpdatedAt().toString()
            ));
            redisTemplate.opsForValue().set("mood:" + userId, json);
        } catch (Exception ignored) {}

        // Trigger Live Activity update to partner via APNs
        Long partnerId = couple.getUserAId().equals(userId)
            ? couple.getUserBId() : couple.getUserAId();
        User partner = userRepo.findById(partnerId).orElse(null);
        if (partner != null && partner.getDeviceToken() != null) {
            liveActivityPush.updateMoodActivity(partner.getDeviceToken(), emoji, text);
        }

        return mood;
    }

    public MoodStatus getPartnerMood(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (user.getPartnerId() == null) throw new BizException(ErrorCode.COUPLE_NOT_BOUND);

        return moodRepo.findTopByUserIdOrderByUpdatedAtDesc(user.getPartnerId())
            .orElse(null);
    }
}
```

```java
// MoodController.java
package com.unibond.mood.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.security.UserPrincipal;
import com.unibond.mood.dto.MoodResponse;
import com.unibond.mood.dto.MoodUpdateRequest;
import com.unibond.mood.entity.MoodStatus;
import com.unibond.mood.service.MoodService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mood")
public class MoodController {
    private final MoodService moodService;

    public MoodController(MoodService moodService) {
        this.moodService = moodService;
    }

    @PostMapping
    public ApiResponse<MoodResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MoodUpdateRequest req) {
        MoodStatus mood = moodService.updateMood(principal.userId(), req.emoji(), req.text());
        return ApiResponse.ok(new MoodResponse(mood.getMoodEmoji(), mood.getMoodText(), mood.getUpdatedAt()));
    }

    @GetMapping("/partner")
    public ApiResponse<MoodResponse> partnerMood(@AuthenticationPrincipal UserPrincipal principal) {
        MoodStatus mood = moodService.getPartnerMood(principal.userId());
        if (mood == null) return ApiResponse.ok(null);
        return ApiResponse.ok(new MoodResponse(mood.getMoodEmoji(), mood.getMoodText(), mood.getUpdatedAt()));
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd unibond-server && ./mvnw test -Dtest=MoodServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add Mood module with Redis caching"
```

---

### Task 11: Stats + Achievement Module

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/stats/entity/DailyStats.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/entity/DailyStatsId.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/entity/Achievement.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/entity/AchievementType.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/repository/DailyStatsRepository.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/repository/AchievementRepository.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/dto/*.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/service/AchievementService.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/service/StatsService.java`
- Create: `unibond-server/src/main/java/com/unibond/stats/controller/StatsController.java`
- Test: `unibond-server/src/test/java/com/unibond/stats/service/AchievementServiceTest.java`

- [ ] **Step 1: Write AchievementService test**

```java
package com.unibond.stats.service;

import com.unibond.stats.entity.AchievementType;
import com.unibond.stats.repository.AchievementRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {
    @Mock private AchievementRepository achievementRepo;
    @Mock private DailyStatsRepository statsRepo;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @InjectMocks private AchievementService service;

    @Test
    void checkAchievements_streak7_unlocks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("streak:1")).thenReturn("7");
        when(achievementRepo.existsByCoupleIdAndType(1L, AchievementType.STREAK_7.name()))
            .thenReturn(false);
        when(achievementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.checkAchievements(1L, 100, 7);

        verify(achievementRepo).save(argThat(a ->
            a.getType().equals(AchievementType.STREAK_7.name())));
    }

    @Test
    void checkAchievements_perfectScore_unlocks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("streak:1")).thenReturn("1");
        when(achievementRepo.existsByCoupleIdAndType(1L, AchievementType.PERFECT_MATCH.name()))
            .thenReturn(false);
        when(achievementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.checkAchievements(1L, 100, 1);

        verify(achievementRepo).save(argThat(a ->
            a.getType().equals(AchievementType.PERFECT_MATCH.name())));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd unibond-server && ./mvnw test -Dtest=AchievementServiceTest`
Expected: FAIL

- [ ] **Step 3: Implement entities and enums**

```java
// AchievementType.java
package com.unibond.stats.entity;

public enum AchievementType {
    FIRST_BIND("命中注定"),
    STREAK_3("初识默契"),
    STREAK_7("默契升温"),
    STREAK_30("心有灵犀"),
    STREAK_100("灵魂伴侣"),
    PERFECT_MATCH("心意相通"),
    HIGH_SCORE_10("默契之星"),
    THEME_FOOD("美食知己"),
    THEME_TRAVEL("旅行搭档"),
    THEME_MEMORY("回忆收藏家"),
    ANNIVERSARY("甜蜜纪念");

    private final String displayName;
    AchievementType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}

// DailyStatsId.java
package com.unibond.stats.entity;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
public class DailyStatsId implements Serializable {
    private Long coupleId;
    private LocalDate statDate;
    public DailyStatsId() {}
    public DailyStatsId(Long coupleId, LocalDate statDate) {
        this.coupleId = coupleId;
        this.statDate = statDate;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyStatsId that)) return false;
        return Objects.equals(coupleId, that.coupleId) && Objects.equals(statDate, that.statDate);
    }
    @Override public int hashCode() { return Objects.hash(coupleId, statDate); }
}

// DailyStats.java
package com.unibond.stats.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name = "daily_stats")
@IdClass(DailyStatsId.class)
public class DailyStats {
    @Id private Long coupleId;
    @Id @Column(name = "stat_date") private LocalDate statDate;
    private int matchScore;
    private int streakDays;
    @Enumerated(EnumType.STRING)
    private com.unibond.quiz.entity.QuizType quizTypePlayed;

    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long c) { this.coupleId = c; }
    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate d) { this.statDate = d; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int s) { this.matchScore = s; }
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int s) { this.streakDays = s; }
    public com.unibond.quiz.entity.QuizType getQuizTypePlayed() { return quizTypePlayed; }
    public void setQuizTypePlayed(com.unibond.quiz.entity.QuizType t) { this.quizTypePlayed = t; }
}

// Achievement.java
package com.unibond.stats.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "achievements")
public class Achievement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long coupleId;
    private String type;
    private Instant unlockedAt;
    @PrePersist void prePersist() { if (unlockedAt == null) unlockedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long c) { this.coupleId = c; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public Instant getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(Instant u) { this.unlockedAt = u; }
}
```

- [ ] **Step 4: Implement repositories, service, DTOs, controller**

```java
// DailyStatsRepository.java
package com.unibond.stats.repository;
import com.unibond.stats.entity.DailyStats;
import com.unibond.stats.entity.DailyStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface DailyStatsRepository extends JpaRepository<DailyStats, DailyStatsId> {
    List<DailyStats> findByCoupleIdAndStatDateBetweenOrderByStatDateDesc(
        Long coupleId, LocalDate start, LocalDate end);
}

// AchievementRepository.java
package com.unibond.stats.repository;
import com.unibond.stats.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByCoupleId(Long coupleId);
    boolean existsByCoupleIdAndType(Long coupleId, String type);
}
```

```java
// AchievementService.java
package com.unibond.stats.service;

import com.unibond.stats.entity.Achievement;
import com.unibond.stats.entity.AchievementType;
import com.unibond.stats.repository.AchievementRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AchievementService {
    private final AchievementRepository achievementRepo;
    private final DailyStatsRepository statsRepo;
    private final StringRedisTemplate redisTemplate;

    public AchievementService(AchievementRepository achievementRepo,
                               DailyStatsRepository statsRepo,
                               StringRedisTemplate redisTemplate) {
        this.achievementRepo = achievementRepo;
        this.statsRepo = statsRepo;
        this.redisTemplate = redisTemplate;
    }

    public List<Achievement> checkAchievements(Long coupleId, int score, int streakDays) {
        List<Achievement> unlocked = new ArrayList<>();

        // Streak achievements
        if (streakDays >= 3) tryUnlock(coupleId, AchievementType.STREAK_3, unlocked);
        if (streakDays >= 7) tryUnlock(coupleId, AchievementType.STREAK_7, unlocked);
        if (streakDays >= 30) tryUnlock(coupleId, AchievementType.STREAK_30, unlocked);
        if (streakDays >= 100) tryUnlock(coupleId, AchievementType.STREAK_100, unlocked);

        // Score achievements
        if (score == 100) tryUnlock(coupleId, AchievementType.PERFECT_MATCH, unlocked);

        return unlocked;
    }

    private void tryUnlock(Long coupleId, AchievementType type, List<Achievement> unlocked) {
        if (!achievementRepo.existsByCoupleIdAndType(coupleId, type.name())) {
            Achievement a = new Achievement();
            a.setCoupleId(coupleId);
            a.setType(type.name());
            unlocked.add(achievementRepo.save(a));
        }
    }
}
```

```java
// DTOs
package com.unibond.stats.dto;
import java.util.List;
public record OverviewResponse(int todayScore, int streakDays, int totalQuizzes,
    double avgScore, List<AchievementResponse> recentAchievements) {}

// AchievementResponse.java
package com.unibond.stats.dto;
import java.time.Instant;
public record AchievementResponse(String type, String displayName,
    boolean unlocked, Instant unlockedAt) {}

// WeeklyResponse.java
package com.unibond.stats.dto;
import java.util.List;
public record WeeklyResponse(List<DayScore> scores, double avgScore, int quizzesCompleted) {}

// DayScore.java
package com.unibond.stats.dto;
import java.time.LocalDate;
public record DayScore(LocalDate date, int score, String quizType) {}
```

```java
// StatsService.java
package com.unibond.stats.service;

import com.unibond.stats.dto.*;
import com.unibond.stats.entity.Achievement;
import com.unibond.stats.entity.AchievementType;
import com.unibond.stats.entity.DailyStats;
import com.unibond.stats.repository.AchievementRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class StatsService {
    private final DailyStatsRepository statsRepo;
    private final AchievementRepository achievementRepo;

    public StatsService(DailyStatsRepository statsRepo, AchievementRepository achievementRepo) {
        this.statsRepo = statsRepo;
        this.achievementRepo = achievementRepo;
    }

    public OverviewResponse getOverview(Long coupleId) {
        LocalDate today = LocalDate.now();
        var todayStats = statsRepo.findById(new com.unibond.stats.entity.DailyStatsId(coupleId, today));
        int todayScore = todayStats.map(DailyStats::getMatchScore).orElse(0);
        int streakDays = todayStats.map(DailyStats::getStreakDays).orElse(0);

        LocalDate weekAgo = today.minusDays(30);
        List<DailyStats> recentStats = statsRepo.findByCoupleIdAndStatDateBetweenOrderByStatDateDesc(
            coupleId, weekAgo, today);
        int totalQuizzes = recentStats.size();
        double avgScore = recentStats.stream().mapToInt(DailyStats::getMatchScore).average().orElse(0);

        List<AchievementResponse> recent = achievementRepo.findByCoupleId(coupleId).stream()
            .sorted((a, b) -> b.getUnlockedAt().compareTo(a.getUnlockedAt()))
            .limit(3)
            .map(a -> {
                AchievementType type = AchievementType.valueOf(a.getType());
                return new AchievementResponse(a.getType(), type.getDisplayName(), true, a.getUnlockedAt());
            })
            .toList();

        return new OverviewResponse(todayScore, streakDays, totalQuizzes, avgScore, recent);
    }

    public List<AchievementResponse> getAchievements(Long coupleId) {
        List<Achievement> unlocked = achievementRepo.findByCoupleId(coupleId);
        return Arrays.stream(AchievementType.values()).map(type -> {
            Achievement a = unlocked.stream()
                .filter(u -> u.getType().equals(type.name()))
                .findFirst().orElse(null);
            return new AchievementResponse(type.name(), type.getDisplayName(),
                a != null, a != null ? a.getUnlockedAt() : null);
        }).toList();
    }

    public WeeklyResponse getWeekly(Long coupleId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<DailyStats> stats = statsRepo.findByCoupleIdAndStatDateBetweenOrderByStatDateDesc(
            coupleId, start, end);

        List<DayScore> scores = stats.stream()
            .map(s -> new DayScore(s.getStatDate(), s.getMatchScore(),
                s.getQuizTypePlayed() != null ? s.getQuizTypePlayed().name() : null))
            .toList();

        double avg = stats.stream().mapToInt(DailyStats::getMatchScore).average().orElse(0);
        return new WeeklyResponse(scores, avg, stats.size());
    }
}
```

```java
// StatsController.java
package com.unibond.stats.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.security.UserPrincipal;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.service.CoupleService;
import com.unibond.stats.dto.*;
import com.unibond.stats.service.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {
    private final StatsService statsService;
    private final CoupleService coupleService;

    public StatsController(StatsService statsService, CoupleService coupleService) {
        this.statsService = statsService;
        this.coupleService = coupleService;
    }

    @GetMapping("/overview")
    public ApiResponse<OverviewResponse> overview(
            @AuthenticationPrincipal UserPrincipal principal) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        return ApiResponse.ok(statsService.getOverview(couple.getId()));
    }

    @GetMapping("/achievements")
    public ApiResponse<List<AchievementResponse>> achievements(
            @AuthenticationPrincipal UserPrincipal principal) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        return ApiResponse.ok(statsService.getAchievements(couple.getId()));
    }

    @GetMapping("/weekly")
    public ApiResponse<WeeklyResponse> weekly(
            @AuthenticationPrincipal UserPrincipal principal) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        return ApiResponse.ok(statsService.getWeekly(couple.getId()));
    }
}
```

- [ ] **Step 5: Run tests**

Run: `cd unibond-server && ./mvnw test -Dtest=AchievementServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add Stats and Achievement module"
```

---

### Task 12: Push Notification Service (APNs)

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/config/ApnsConfig.java`
- Create: `unibond-server/src/main/java/com/unibond/push/service/PushService.java`
- Create: `unibond-server/src/main/java/com/unibond/push/service/LiveActivityPushService.java`

- [ ] **Step 1: Add APNs config properties to application.yml**

Append to `application.yml`:

```yaml
app:
  apns:
    key-path: ${APNS_KEY_PATH:}
    key-id: ${APNS_KEY_ID:}
    team-id: ${APNS_TEAM_ID:}
    topic: ${APNS_TOPIC:com.unibond.app}
    production: false
```

- [ ] **Step 2: Implement ApnsConfig**

```java
package com.unibond.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.File;

@Configuration
@ConditionalOnProperty(name = "app.apns.key-path")
public class ApnsConfig {
    @Bean
    public ApnsClient apnsClient(
            @Value("${app.apns.key-path}") String keyPath,
            @Value("${app.apns.key-id}") String keyId,
            @Value("${app.apns.team-id}") String teamId,
            @Value("${app.apns.production}") boolean production) throws Exception {
        return new ApnsClientBuilder()
            .setApnsServer(production
                ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
            .setSigningKey(com.eatthepath.pushy.apns.auth.ApnsSigningKey.loadFromPkcs8File(
                new File(keyPath), teamId, keyId))
            .build();
    }
}
```

- [ ] **Step 3: Implement PushService**

```java
package com.unibond.push.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PushService {
    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    @Autowired(required = false)
    private ApnsClient apnsClient;

    @Value("${app.apns.topic:com.unibond.app}")
    private String topic;

    public void sendPush(String deviceToken, String title, String body) {
        if (apnsClient == null || deviceToken == null) return;

        try {
            String payload = new SimpleApnsPayloadBuilder()
                .setAlertTitle(title)
                .setAlertBody(body)
                .setSound("default")
                .build();

            var notification = new SimpleApnsPushNotification(
                TokenUtil.sanitizeTokenString(deviceToken), topic, payload);
            apnsClient.sendNotification(notification).whenComplete((resp, cause) -> {
                if (cause != null) {
                    log.error("APNs send failed", cause);
                } else if (!resp.isAccepted()) {
                    log.warn("APNs rejected: {}", resp.getRejectionReason());
                }
            });
        } catch (Exception e) {
            log.error("Failed to send push", e);
        }
    }
}
```

- [ ] **Step 4: Implement LiveActivityPushService**

```java
package com.unibond.push.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Service
public class LiveActivityPushService {
    private static final Logger log = LoggerFactory.getLogger(LiveActivityPushService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private ApnsClient apnsClient;

    @Value("${app.apns.topic:com.unibond.app}")
    private String topic;

    public void updateMoodActivity(String pushToken, String emoji, String text) {
        if (apnsClient == null || pushToken == null) return;

        try {
            Map<String, Object> payload = Map.of(
                "aps", Map.of(
                    "timestamp", Instant.now().getEpochSecond(),
                    "event", "update",
                    "content-state", Map.of(
                        "emoji", emoji != null ? emoji : "",
                        "text", text != null ? text : "",
                        "updatedAt", Instant.now().toString()
                    )
                )
            );
            String payloadJson = objectMapper.writeValueAsString(payload);

            var notification = new SimpleApnsPushNotification(
                TokenUtil.sanitizeTokenString(pushToken),
                topic + ".push-type.liveactivity",
                payloadJson);
            apnsClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to update Live Activity", e);
        }
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add APNs push notification and Live Activity support"
```

---

### Task 13: Rate Limiting

**Files:**
- Create: `unibond-server/src/main/java/com/unibond/common/ratelimit/RateLimitInterceptor.java`
- Create: `unibond-server/src/main/java/com/unibond/config/RateLimitConfig.java`

- [ ] **Step 1: Implement rate limiting interceptor using Bucket4j**

```java
package com.unibond.common.ratelimit;

import com.unibond.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String key = resolveKey(req);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(req.getRequestURI()));

        if (bucket.tryConsume(1)) {
            return true;
        }

        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(
            ErrorResponse.of("RATE_LIMIT_EXCEEDED", "请求频率超限")));
        return false;
    }

    private String resolveKey(HttpServletRequest req) {
        // Use userId if authenticated, otherwise IP
        var auth = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.unibond.common.security.UserPrincipal p) {
            return "user:" + p.userId() + ":" + req.getRequestURI();
        }
        return "ip:" + req.getRemoteAddr() + ":" + req.getRequestURI();
    }

    private Bucket createBucket(String uri) {
        if (uri.contains("/auth/email/send")) {
            return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(1).refillGreedy(1, Duration.ofSeconds(60)).build())
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofHours(1)).build())
                .build();
        }
        if (uri.contains("/auth/email/login")) {
            return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build())
                .build();
        }
        if (uri.contains("/couple/bind")) {
            return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1)).build())
                .build();
        }
        if (uri.contains("/auth/")) {
            return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(30).refillGreedy(30, Duration.ofMinutes(1)).build())
                .build();
        }
        // Default: 60/min for authenticated endpoints
        return Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build())
            .build();
    }
}
```

- [ ] **Step 2: Register interceptor in WebConfig**

Add to `WebConfig.java`:

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new RateLimitInterceptor())
        .addPathPatterns("/api/**");
}
```

- [ ] **Step 3: Commit**

```bash
git add unibond-server/src/
git commit -m "feat: add rate limiting with Bucket4j"
```

---

### Task 14: Fallback Question Pool Seed Data

**Files:**
- Create: `unibond-server/src/main/resources/db/migration/V2__seed_question_pool.sql`

- [ ] **Step 1: Write seed data migration**

```sql
-- V2__seed_question_pool.sql
-- Seed fallback question pool with starter questions

-- BLIND mode questions
INSERT INTO question_pool (category, quiz_type, question, options, difficulty) VALUES
('日常', 'BLIND', '周末最理想的活动是？', '["宅家看剧","出门逛街","约朋友聚餐","户外运动"]', 1),
('日常', 'BLIND', '最喜欢的季节是？', '["春天","夏天","秋天","冬天"]', 1),
('日常', 'BLIND', '压力大时会怎么做？', '["吃好吃的","运动","找人倾诉","独处安静"]', 1),
('美食', 'BLIND', '最爱的菜系是？', '["川菜","粤菜","日料","西餐"]', 1),
('美食', 'BLIND', '早餐偏好是？', '["中式早餐","面包牛奶","不吃早餐","随便吃点"]', 1),
('生活', 'BLIND', '理想的居住城市类型？', '["大城市","小城市","海边","山里"]', 1),
('生活', 'BLIND', '最想养的宠物？', '["猫","狗","都想养","都不想养"]', 1),
('情感', 'BLIND', '吵架后谁先道歉？', '["我先","对方先","冷战到忘记","找朋友评理"]', 2),
('情感', 'BLIND', '最打动你的告白方式？', '["写信","当面说","准备惊喜","日常陪伴"]', 2),
('兴趣', 'BLIND', '最喜欢的电影类型？', '["喜剧","爱情","科幻","悬疑"]', 1),

-- GUESS mode questions
('日常', 'GUESS', '对方最怕什么？', '["蟑螂","蛇","打针","黑暗"]', 1),
('日常', 'GUESS', '对方的口头禅是？', '["好的","随便","不知道","真的吗"]', 1),
('美食', 'GUESS', '对方最爱的零食是？', '["薯片","巧克力","水果","坚果"]', 1),
('美食', 'GUESS', '对方不能接受的食物？', '["香菜","苦瓜","榴莲","内脏"]', 1),
('情感', 'GUESS', '对方最在意的纪念日？', '["生日","在一起纪念日","第一次约会","都在意"]', 2),
('生活', 'GUESS', '对方睡前最后做的事？', '["刷手机","看书","听音乐","发呆"]', 1),
('生活', 'GUESS', '对方起床后第一件事？', '["看手机","洗漱","发呆","喝水"]', 1),
('兴趣', 'GUESS', '对方最想去的国家？', '["日本","法国","冰岛","新西兰"]', 1),
('兴趣', 'GUESS', '对方最擅长做的菜？', '["炒蛋","泡面","啥都不会","大厨级别"]', 1),
('情感', 'GUESS', '对方觉得你最可爱的时刻？', '["撒娇时","认真工作时","睡着时","吃东西时"]', 2),

-- THEME mode questions (美食主题)
('美食', 'THEME', '你觉得最浪漫的餐厅氛围是？', '["烛光晚餐","露天花园","日式居酒屋","家里做饭"]', 1),
('美食', 'THEME', '一起做饭时你负责？', '["主厨","帮手","洗碗","拍照"]', 1),
('美食', 'THEME', '最想一起尝试的美食体验？', '["路边摊扫街","米其林餐厅","自助烧烤","烘焙蛋糕"]', 1),
('美食', 'THEME', '火锅蘸料偏好？', '["麻酱","油碟","酱油醋","啥都加"]', 1),
('美食', 'THEME', '奶茶甜度？', '["全糖","七分糖","三分糖","无糖"]', 1);
```

- [ ] **Step 2: Verify migration runs**

Run: `cd unibond-server && ./mvnw spring-boot:run`
Expected: V2 migration executes, 25 questions seeded

- [ ] **Step 3: Commit**

```bash
git add unibond-server/src/main/resources/db/migration/V2__seed_question_pool.sql
git commit -m "feat: seed fallback question pool with 25 starter questions"
```

---

### Task 15: Integration Smoke Test

**Files:**
- Create: `unibond-server/src/test/java/com/unibond/UnibondApplicationTest.java`

- [ ] **Step 1: Write application context load test**

```java
package com.unibond;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UnibondApplicationTest {
    @Test
    void contextLoads() {
        // Verifies all beans wire up correctly
    }
}
```

- [ ] **Step 2: Create application-test.yml**

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  data:
    redis:
      host: localhost
      port: 6379
  flyway:
    enabled: false

app:
  jwt:
    secret: test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm
    access-token-expiry: 7200000
    refresh-token-expiry: 2592000000
  invite-code:
    charset: "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    length: 6
    max-retries: 3
```

- [ ] **Step 3: Run all tests**

Run: `cd unibond-server && ./mvnw test`
Expected: All tests pass (context may fail if Redis not available — that's expected for CI, document accordingly)

- [ ] **Step 4: Commit**

```bash
git add unibond-server/src/test/
git commit -m "test: add application context smoke test"
```

---

### Task 16: .gitignore Update + Final Verification

- [ ] **Step 1: Update .gitignore for the full project**

Append to root `.gitignore`:

```
# IDE
.idea/
*.iml
.vscode/

# Maven
target/

# Spring Boot
*.log
logs/

# Docker
docker-compose.override.yml

# Secrets
*.p8
*.pem
.env
```

- [ ] **Step 2: Run full test suite**

Run: `cd unibond-server && ./mvnw clean test`
Expected: All unit tests pass

- [ ] **Step 3: Commit**

```bash
git add .gitignore
git commit -m "chore: update .gitignore for Java/Spring Boot project"
```
