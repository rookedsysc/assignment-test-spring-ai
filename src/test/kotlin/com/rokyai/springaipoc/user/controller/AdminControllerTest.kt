package com.rokyai.springaipoc.user.controller

import com.rokyai.springaipoc.common.BaseIntegrationTest
import com.rokyai.springaipoc.user.entity.Role
import com.rokyai.springaipoc.user.entity.User
import com.rokyai.springaipoc.user.repository.UserRepository
import com.rokyai.springaipoc.user.util.JwtUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@DisplayName("관리자 API 테스트")
class AdminControllerTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    private lateinit var adminUserId: UUID
    private lateinit var adminToken: String
    private lateinit var memberUserId: UUID
    private lateinit var memberToken: String

    @BeforeEach
    fun setup() {
        val adminUser = User(
            email = "admin@test.com",
            password = passwordEncoder.encode("admin123!"),
            name = "테스트 관리자",
            role = Role.ADMIN
        )
        val savedAdmin = userRepository.save(adminUser).block()!!
        adminUserId = savedAdmin.id!!
        adminToken = jwtUtil.generateToken(adminUserId, savedAdmin.email, savedAdmin.role)

        val memberUser = User(
            email = "member@test.com",
            password = passwordEncoder.encode("member123!"),
            name = "테스트 회원",
            role = Role.MEMBER
        )
        val savedMember = userRepository.save(memberUser).block()!!
        memberUserId = savedMember.id!!
        memberToken = jwtUtil.generateToken(memberUserId, savedMember.email, savedMember.role)
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll().block()
    }

    @Test
    @DisplayName("관리자가 일반 회원을 관리자로 승격 시 성공한다")
    fun promoteUserToAdminByAdmin() {
        // Given - 회원을 관리자로 승격 요청
        val request = """
            {
                "userId": "$memberUserId"
            }
        """.trimIndent()

        // When - 관리자가 회원 승격 API 호출
        webTestClient.post()
            .uri("/admin/users/promote")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            // Then - 응답 검증
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(memberUserId.toString())
            .jsonPath("$.email").isEqualTo("member@test.com")
            .jsonPath("$.role").isEqualTo(Role.ADMIN.name)

        // Then - 데이터베이스 검증
        val updatedUser = userRepository.findById(memberUserId).block()
        assert(updatedUser?.role == Role.ADMIN)
    }

    @Test
    @DisplayName("일반 회원이 다른 회원을 관리자로 승격 시도 시 403 Forbidden을 반환한다")
    fun promoteUserToAdminByMember() {
        // Given - 다른 회원 생성
        val anotherMember = User(
            email = "another@test.com",
            password = passwordEncoder.encode("test123!"),
            name = "또 다른 회원",
            role = Role.MEMBER
        )
        val savedAnother = userRepository.save(anotherMember).block()!!

        val request = """
            {
                "userId": "${savedAnother.id}"
            }
        """.trimIndent()

        // When & Then - 일반 회원 토큰으로 요청 시 403 반환
        webTestClient.post()
            .uri("/admin/users/promote")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @DisplayName("존재하지 않는 회원을 관리자로 승격 시도 시 404 Not Found를 반환한다")
    fun promoteNonExistentUser() {
        // Given - 존재하지 않는 UUID
        val nonExistentId = UUID.randomUUID()
        val request = """
            {
                "userId": "$nonExistentId"
            }
        """.trimIndent()

        // When & Then - 404 반환
        webTestClient.post()
            .uri("/admin/users/promote")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("인증 없이 회원 승격 시도 시 401 Unauthorized를 반환한다")
    fun promoteUserWithoutAuth() {
        // Given - 승격 요청
        val request = """
            {
                "userId": "$memberUserId"
            }
        """.trimIndent()

        // When & Then - 토큰 없이 요청 시 401 반환
        webTestClient.post()
            .uri("/admin/users/promote")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @DisplayName("필수 필드(userId)가 누락된 경우 400 Bad Request를 반환한다")
    fun promoteUserWithoutUserId() {
        // Given - userId 누락된 요청
        val request = """
            {}
        """.trimIndent()

        // When & Then - 400 반환
        webTestClient.post()
            .uri("/admin/users/promote")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
    }
}
