package com.rokyai.springaipoc.user.controller

import com.rokyai.springaipoc.common.BaseIntegrationTest
import com.rokyai.springaipoc.user.entity.Role
import com.rokyai.springaipoc.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@DisplayName("회원가입 API 테스트")
class UserControllerTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll().block()
    }

    @Test
    @DisplayName("정상적인 회원가입 요청 시 사용자가 생성되고 201 응답을 반환한다")
    fun registerSuccess() {
        // Given - 회원가입 요청 데이터 준비
        val request = """
            {
                "email": "test@example.com",
                "password": "password123!",
                "name": "홍길동"
            }
        """.trimIndent()

        // When - 회원가입 API 호출
        webTestClient.post()
            .uri("/user/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            // Then - 응답 검증
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").isNotEmpty
            .jsonPath("$.email").isEqualTo("test@example.com")
            .jsonPath("$.name").isEqualTo("홍길동")
            .jsonPath("$.role").isEqualTo(Role.MEMBER.name)
            .jsonPath("$.createdAt").isNotEmpty

        // Then - 데이터베이스 검증
        val savedUser = userRepository.findByEmail("test@example.com").block()
        assert(savedUser != null)
        assert(savedUser?.email == "test@example.com")
        assert(savedUser?.name == "홍길동")
        assert(savedUser?.role == Role.MEMBER)
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시도 시 409 Conflict를 반환한다")
    fun registerWithDuplicateEmail() {
        // Given - 기존 사용자 생성
        val existingRequest = """
            {
                "email": "duplicate@example.com",
                "password": "password123!",
                "name": "기존사용자"
            }
        """.trimIndent()

        webTestClient.post()
            .uri("/user/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(existingRequest)
            .exchange()
            .expectStatus().isCreated

        // When - 동일한 이메일로 재가입 시도
        val duplicateRequest = """
            {
                "email": "duplicate@example.com",
                "password": "newpassword456!",
                "name": "새사용자"
            }
        """.trimIndent()

        // Then - 409 Conflict 응답 확인
        webTestClient.post()
            .uri("/user/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(duplicateRequest)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody()
            .jsonPath("$.message").isNotEmpty
    }

    @Test
    @DisplayName("필수 필드(이메일)가 누락된 경우 400 Bad Request를 반환한다")
    fun registerWithoutEmail() {
        // Given - 이메일이 누락된 요청
        val request = """
            {
                "password": "password123!",
                "name": "홍길동"
            }
        """.trimIndent()

        // When & Then - 400 Bad Request 응답 확인
        webTestClient.post()
            .uri("/user/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("필수 필드(패스워드)가 누락된 경우 400 Bad Request를 반환한다")
    fun registerWithoutPassword() {
        // Given - 패스워드가 누락된 요청
        val request = """
            {
                "email": "test@example.com",
                "name": "홍길동"
            }
        """.trimIndent()

        // When & Then - 400 Bad Request 응답 확인
        webTestClient.post()
            .uri("/user/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("필수 필드(이름)가 누락된 경우 400 Bad Request를 반환한다")
    fun registerWithoutName() {
        // Given - 이름이 누락된 요청
        val request = """
            {
                "email": "test@example.com",
                "password": "password123!"
            }
        """.trimIndent()

        // When & Then - 400 Bad Request 응답 확인
        webTestClient.post()
            .uri("/user/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("잘못된 이메일 형식인 경우 400 Bad Request를 반환한다")
    fun registerWithInvalidEmailFormat() {
        // Given - 잘못된 이메일 형식
        val request = """
            {
                "email": "invalid-email",
                "password": "password123!",
                "name": "홍길동"
            }
        """.trimIndent()

        // When & Then - 400 Bad Request 응답 확인
        webTestClient.post()
            .uri("/user/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }
}
