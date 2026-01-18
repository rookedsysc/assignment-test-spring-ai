# Spring Boot WebFlux JWT 인증 시스템

## 개요

Spring Boot WebFlux, R2DBC, PostgreSQL을 사용한 Reactive JWT 인증 시스템 구현 프로젝트입니다.

## 기술 스택

- **Language**: Kotlin 1.9.25
- **Framework**: Spring Boot 3.5.9 (WebFlux)
- **Database**: PostgreSQL 15 (R2DBC)
- **Security**: Spring Security + JWT (JJWT 0.12.6)
- **Test**: JUnit 5, TestContainers, WebTestClient
- **API Documentation**: SpringDoc OpenAPI 2.8.15

## 주요 기능

### 1. 회원가입 (POST /user/auth/register)
- 이메일, 패스워드, 이름으로 회원가입
- 패스워드는 BCrypt로 암호화 저장
- 이메일 중복 검증
- 기본 권한: MEMBER

### 2. 로그인 (POST /user/auth/login)
- 이메일, 패스워드로 로그인
- JWT 토큰 발급 (유효기간 1시간)
- Bearer 토큰 방식

### 3. 프로필 조회 (GET /user/profile)
- JWT 인증 필요
- 로그인한 사용자의 프로필 정보 조회

## 데이터베이스 스키마

### users 테이블
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

## API 명세

### 회원가입
```http
POST /user/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123!",
  "name": "홍길동"
}
```

**Response (201 Created)**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "홍길동",
  "role": "MEMBER",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**Error Cases**
- 400: 필수값 누락, 이메일 형식 오류, 패스워드 길이 부족
- 409: 이메일 중복

### 로그인
```http
POST /user/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123!"
}
```

**Response (200 OK)**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Error Cases**
- 400: 필수값 누락
- 401: 패스워드 불일치
- 404: 사용자를 찾을 수 없음

### 프로필 조회
```http
GET /user/profile
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "홍길동",
  "role": "MEMBER",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**Error Cases**
- 401: 토큰 없음, 만료, 또는 잘못된 토큰
- 404: 사용자를 찾을 수 없음

## 프로젝트 구조

```
src/main/kotlin/com/rokyai/springaipoc/
├── config/
│   └── SecurityConfig.kt           # Spring Security 설정
├── user/
│   ├── controller/
│   │   ├── UserController.kt       # 회원가입, 로그인 API
│   │   └── ProfileController.kt    # 프로필 조회 API
│   ├── dto/
│   │   ├── UserRegisterRequest.kt
│   │   ├── LoginRequest.kt
│   │   ├── UserResponse.kt
│   │   └── TokenResponse.kt
│   ├── entity/
│   │   ├── User.kt
│   │   └── Role.kt
│   ├── exception/
│   │   └── UserException.kt
│   ├── filter/
│   │   └── JwtAuthenticationFilter.kt  # JWT 인증 필터
│   ├── mapper/
│   │   └── UserMapper.kt
│   ├── repository/
│   │   └── UserRepository.kt
│   ├── service/
│   │   ├── UserService.kt
│   │   └── AuthService.kt
│   └── util/
│       └── JwtUtil.kt               # JWT 생성/검증
└── chat/
    └── exception/
        └── GlobalExceptionHandler.kt   # 전역 예외 처리
```

## 공통 코드 및 구성요소

### ErrorResponse DTO
**위치**: `src/main/kotlin/com/rokyai/springaipoc/chat/exception/ErrorResponse.kt`

**사용법**: 모든 에러 응답에 사용되는 공통 DTO
```kotlin
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)
```

**반환값**:
- `status`: HTTP 상태 코드 (400, 401, 404, 409 등)
- `error`: HTTP 상태 메시지
- `message`: 구체적인 에러 메시지
- `path`: 요청 경로

### GlobalExceptionHandler
**위치**: `src/main/kotlin/com/rokyai/springaipoc/chat/exception/GlobalExceptionHandler.kt`

**동작**: 애플리케이션 전역에서 발생하는 예외를 처리하고 ErrorResponse 형식으로 변환

**처리하는 예외**:
- `ServerWebInputException`: 잘못된 입력 데이터 (400)
- `DecodingException`: JSON 역직렬화 실패 (400)
- `WebExchangeBindException`: Validation 실패 (400)
- `DuplicateEmailException`: 이메일 중복 (409)
- `UserNotFoundException`: 사용자 없음 (404)
- `InvalidPasswordException`: 패스워드 불일치 (401)
- `IllegalArgumentException`: 잘못된 파라미터 (400)
- `IllegalStateException`: 내부 상태 오류 (500)
- `Exception`: 기타 예외 (500)

### JwtUtil
**위치**: `src/main/kotlin/com/rokyai/springaipoc/user/util/JwtUtil.kt`

**사용법**: JWT 토큰 생성 및 검증
```kotlin
// 토큰 생성
val token = jwtUtil.generateToken(userId, email, role)

// 토큰 검증
val isValid = jwtUtil.validateToken(token)

// 토큰에서 정보 추출
val userId = jwtUtil.getUserIdFromToken(token)
val email = jwtUtil.getEmailFromToken(token)
val role = jwtUtil.getRoleFromToken(token)
```

**파라미터**:
- `userId`: 사용자 UUID
- `email`: 이메일 주소
- `role`: 사용자 권한 (MEMBER, ADMIN)

**반환값**: JWT 토큰 문자열

**설정**:
- Secret: `application.yml`의 `jwt.secret` (기본값 제공)
- 만료시간: `jwt.expiration` (기본 1시간 = 3600000ms)

### Role Enum
**위치**: `src/main/kotlin/com/rokyai/springaipoc/user/entity/Role.kt`

**값**:
- `MEMBER`: 일반 회원
- `ADMIN`: 관리자

## 환경 설정

### application.yml
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/assignment_test_db
    username: root
    password: 1234
  sql:
    init:
      mode: always

jwt:
  secret: ${JWT_SECRET:mySecretKeyForJWTTokenGenerationAndValidationPurposesOnly123456789}
  expiration: 3600000  # 1 hour
```

### 환경 변수 (선택사항)
- `JWT_SECRET`: JWT 서명에 사용할 비밀키 (미설정 시 기본값 사용)

## 테스트 실행

### 전체 테스트
```bash
./gradlew test
```

### 개별 테스트
```bash
# 회원가입 테스트
./gradlew test --tests "com.rokyai.springaipoc.user.controller.UserControllerTest"

# 로그인 테스트
./gradlew test --tests "com.rokyai.springaipoc.user.controller.AuthControllerTest"

# JWT 인증 필터 테스트
./gradlew test --tests "com.rokyai.springaipoc.user.controller.ProtectedApiTest"
```

## 테스트 커버리지

### UserControllerTest (회원가입)
- ✅ 정상적인 회원가입
- ✅ 이메일 중복 검증
- ✅ 필수 필드 누락 (이메일, 패스워드, 이름)
- ✅ 이메일 형식 검증

### AuthControllerTest (로그인)
- ✅ 정상적인 로그인 및 JWT 발급
- ✅ 존재하지 않는 이메일
- ✅ 잘못된 패스워드
- ✅ 필수 필드 누락 (이메일, 패스워드)

### ProtectedApiTest (JWT 인증)
- ✅ 유효한 JWT 토큰으로 API 접근
- ✅ 토큰 없이 API 접근 차단
- ✅ 잘못된 형식의 토큰 차단
- ✅ Bearer 접두사 없는 토큰 차단

## 실행 방법

### 1. PostgreSQL 실행
```bash
docker-compose up -d
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3. API 문서 확인
브라우저에서 `http://localhost:8080/swagger-ui.html` 접속

