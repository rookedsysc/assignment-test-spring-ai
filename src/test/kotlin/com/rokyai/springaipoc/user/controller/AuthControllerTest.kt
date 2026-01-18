package com.rokyai.springaipoc.user.controller

import com.rokyai.springaipoc.common.BaseIntegrationTest
import com.rokyai.springaipoc.user.entity.Role
import com.rokyai.springaipoc.user.entity.User
import com.rokyai.springaipoc.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder

@DisplayName("로그인 API 테스트")
class AuthControllerTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setup() {
        val user = User(
            email = "test@example.com",
            password = passwordEncoder.encode("password123!"),
            name = "테스트 사용자",
            role = Role.MEMBER
        )
        userRepository.save(user).block()
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll().block()
    }

    @Test
    @DisplayName("올바른 이메일과 패스워드로 로그인 시 JWT 토큰을 반환한다")
    fun loginSuccess() {
        // Given - 로그인 요청 데이터 준비
        val request = """
            {
                "email": "test@example.com",
                "password": "password123!"
            }
        """.trimIndent()

        // When - 로그인 API 호출
        webTestClient.post()
            .uri("/user/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            // Then - 응답 검증
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .jsonPath("$.tokenType").isEqualTo("Bearer")
            .jsonPath("$.expiresIn").isNumber
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시도 시 404 Not Found를 반환한다")
    fun loginWithNonExistentEmail() {
        // Given - 존재하지 않는 이메일로 로그인 요청
        val request = """
            {
                "email": "nonexistent@example.com",
                "password": "password123!"
            }
        """.trimIndent()

        // When & Then - 404 Not Found 응답 확인
        webTestClient.post()
            .uri("/user/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$.message").isNotEmpty
    }

    @Test
    @DisplayName("잘못된 패스워드로 로그인 시도 시 401 Unauthorized를 반환한다")
    fun loginWithWrongPassword() {
        // Given - 잘못된 패스워드로 로그인 요청
        val request = """
            {
                "email": "test@example.com",
                "password": "wrongpassword!"
            }
        """.trimIndent()

        // When & Then - 401 Unauthorized 응답 확인
        webTestClient.post()
            .uri("/user/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.message").isNotEmpty
    }

    @Test
    @DisplayName("필수 필드(이메일)가 누락된 경우 400 Bad Request를 반환한다")
    fun loginWithoutEmail() {
        // Given - 이메일이 누락된 요청
        val request = """
            {
                "password": "password123!"
            }
        """.trimIndent()

        // When & Then - 400 Bad Request 응답 확인
        webTestClient.post()
            .uri("/user/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("필수 필드(패스워드)가 누락된 경우 400 Bad Request를 반환한다")
    fun loginWithoutPassword() {
        // Given - 패스워드가 누락된 요청
        val request = """
            {
                "email": "test@example.com"
            }
        """.trimIndent()

        // When & Then - 400 Bad Request 응답 확인
        webTestClient.post()
            .uri("/user/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }
}
