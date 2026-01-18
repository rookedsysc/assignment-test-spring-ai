## 주요 설계 원칙

- **TDD (Test-Driven Development)**: 테스트를 먼저 작성하고 구현
- **SOLID 원칙**: 단일 책임, 개방-폐쇄 원칙 등 준수
- **Reactive Programming**: WebFlux와 R2DBC를 사용한 비동기 처리
- **Security Best Practices**: BCrypt 패스워드 암호화, JWT 토큰 기반 인증
- **API 설계**: RESTful API, role 기반 prefix (user, admin)

## 구현 과정 및 고민 사항

### TDD 접근 방식
요구사항을 받은 후 구현보다 테스트를 먼저 작성하는 방식으로 진행했습니다.

**진행 순서**:
1. 회원가입 테스트 작성 (성공 케이스, 실패 케이스)
2. 테스트를 통과시키기 위한 최소한의 구현 (DTO → Service → Controller)
3. 로그인 테스트 작성
4. JWT 생성 및 인증 로직 구현
5. JWT 인증 필터 테스트 작성
6. 보호된 엔드포인트 구현

이 방식을 통해 요구사항을 명확히 정의하고, 구현 누락을 방지했습니다.

### WebFlux에서의 예외 처리

**문제점**: Kotlin DTO의 필수 파라미터 누락 시 `MissingKotlinParameterException`이 발생하지만, WebFlux 환경에서는 이 예외가 `ServerWebInputException`이나 `DecodingException`으로 래핑되어 500 에러가 발생했습니다.

**해결 방법**:
```kotlin
@ExceptionHandler(ServerWebInputException::class)
fun handleServerWebInputException(ex: ServerWebInputException, exchange: ServerWebExchange) {
    val cause = ex.cause
    val message = when (cause) {
        is MissingKotlinParameterException -> {
            val fieldName = cause.parameter.name ?: "unknown"
            "필수 필드가 누락되었습니다: $fieldName"
        }
        else -> ex.reason ?: "잘못된 요청 데이터입니다."
    }
    // ... 400 에러 반환
}
```

GlobalExceptionHandler에 WebFlux 특화 예외 핸들러를 추가하여 적절한 400 에러를 반환하도록 처리했습니다.

### Reactive Security Context 관리

**고민**: Spring MVC와 달리 WebFlux는 Reactive 스트림 기반이므로 SecurityContext 관리 방식이 다릅니다.

**적용 방안**:
1. **Stateless 설정**: JWT 기반 인증이므로 세션을 사용하지 않도록 `NoOpServerSecurityContextRepository` 설정
2. **Context 전파**: `ReactiveSecurityContextHolder.withAuthentication()`을 사용하여 Reactive Context에 인증 정보 주입
3. **필터 위치**: `SecurityWebFiltersOrder.AUTHENTICATION` 위치에 JWT 필터 추가

```kotlin
chain.filter(exchange)
    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
```

### TestContainers 통합

**고민**: R2DBC를 사용하는 Reactive 애플리케이션의 통합 테스트 환경 구성

**해결**:
- PostgreSQL 15 TestContainer 사용
- `BaseIntegrationTest` 추상 클래스 생성으로 테스트 간 중복 제거
- `@DynamicPropertySource`로 동적으로 R2DBC URL 주입
- 각 테스트 클래스마다 `@AfterEach`에서 데이터 정리

**주의사항**: 여러 테스트 클래스를 동시에 실행하면 컨테이너 연결 문제가 발생할 수 있어, 개별 테스트 클래스 단위로 실행하는 것을 권장합니다.

### API 엔드포인트 설계

**고민**: 요구사항에서 user/admin role을 구분하라는 내용이 있었지만, 엔드포인트를 어떻게 분리할지 고민했습니다.

**결정 사항**:
- `/user/auth/*`: 일반 사용자 인증 관련 (회원가입, 로그인)
- `/user/*`: 일반 사용자 기능 (프로필 조회 등)
- `/admin/auth/*`: 관리자 인증 (향후 확장)
- `/admin/*`: 관리자 전용 기능 (향후 확장)

이렇게 prefix로 role을 구분하여 향후 관리자 기능 추가 시 확장이 용이하도록 구조화했습니다.

### JWT 설정값 관리

**고민**: JWT Secret Key와 만료 시간을 하드코딩할지, 설정 파일로 관리할지 결정이 필요했습니다.

**적용**:
```yaml
jwt:
  secret: ${JWT_SECRET:defaultSecretKey...}
  expiration: 3600000
```

- 환경 변수(`JWT_SECRET`)를 우선 사용하고, 없을 경우 기본값 제공
- 운영 환경에서는 환경 변수로 안전하게 관리 가능
- 개발/테스트 환경에서는 기본값으로 즉시 실행 가능

### 구현 우선순위

요구사항을 분석하여 다음 순서로 구현했습니다:

1. **핵심 기능**: 회원가입, 로그인, JWT 발급 (필수 요구사항)
2. **보안**: JWT 인증 필터, 비밀번호 암호화
3. **검증**: 예외 처리, Validation
4. **테스트**: TDD 방식으로 각 단계마다 테스트 작성
5. **문서화**: README, Swagger 문서

이를 통해 요구사항의 핵심부터 단계적으로 구현하고, 각 단계마다 동작을 검증했습니다.

## 보안 고려사항

1. **패스워드 암호화**: BCrypt 사용 (강도 10)
2. **JWT 서명**: HMAC-SHA256 알고리즘
3. **토큰 만료**: 1시간 (설정 가능)
4. **CSRF 방지**: Stateless JWT 방식으로 CSRF 비활성화
5. **인증 필터**: 회원가입/로그인 외 모든 API 인증 필요
