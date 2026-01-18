package com.rokyai.springaipoc.user.controller

import com.rokyai.springaipoc.chat.entity.ChatHistory
import com.rokyai.springaipoc.chat.entity.Thread
import com.rokyai.springaipoc.chat.repository.ChatHistoryRepository
import com.rokyai.springaipoc.chat.repository.ThreadRepository
import com.rokyai.springaipoc.common.BaseIntegrationTest
import com.rokyai.springaipoc.user.entity.LoginHistory
import com.rokyai.springaipoc.user.entity.Role
import com.rokyai.springaipoc.user.entity.User
import com.rokyai.springaipoc.user.repository.LoginHistoryRepository
import com.rokyai.springaipoc.user.repository.UserRepository
import com.rokyai.springaipoc.user.util.JwtUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

@DisplayName("관리자 분석 API 테스트")
class AdminAnalyticsControllerTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var loginHistoryRepository: LoginHistoryRepository

    @Autowired
    private lateinit var chatHistoryRepository: ChatHistoryRepository

    @Autowired
    private lateinit var threadRepository: ThreadRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    private lateinit var adminToken: String
    private lateinit var memberToken: String

    @BeforeEach
    fun setup() {
        val adminUser = User(
            email = "admin@analytics.com",
            password = passwordEncoder.encode("admin123!"),
            name = "관리자",
            role = Role.ADMIN
        )
        val savedAdmin = userRepository.save(adminUser).block()!!
        adminToken = jwtUtil.generateToken(savedAdmin.id!!, savedAdmin.email, savedAdmin.role)

        val memberUser = User(
            email = "member@analytics.com",
            password = passwordEncoder.encode("member123!"),
            name = "회원",
            role = Role.MEMBER
        )
        val savedMember = userRepository.save(memberUser).block()!!
        memberToken = jwtUtil.generateToken(savedMember.id!!, savedMember.email, savedMember.role)
    }

    @AfterEach
    fun cleanup() {
        chatHistoryRepository.deleteAll().block()
        threadRepository.deleteAll().block()
        loginHistoryRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }

    @Test
    @DisplayName("관리자는 사용자 활동 기록을 조회할 수 있다")
    fun getDailyActivityStats() {
        // Given
        // 최근 24시간 내 가입한 유저 (setup에서 2명 가입함)
        
        // 최근 24시간 내 로그인 기록 추가
        val userId = userRepository.findAll().blockFirst()!!.id!!
        loginHistoryRepository.save(LoginHistory(userId = userId, createdAt = Instant.now())).block()
        
        // 최근 24시간 내 채팅 기록 추가
        val thread = threadRepository.save(Thread(userId = userId.toString())).block()!!
        chatHistoryRepository.save(
            ChatHistory(
                threadId = thread.id!!,
                userId = userId.toString(),
                userMessage = "Hello",
                assistantMessage = "Hi",
                createdAt = OffsetDateTime.now()
            )
        ).block()

        // When
        webTestClient.get()
            .uri("/admin/analytics/activity")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.signupCount").value<Int> { count -> assert(count >= 2) } // admin + member
            .jsonPath("$.loginCount").isEqualTo(1)
            .jsonPath("$.chatCount").isEqualTo(1)
    }

    @Test
    @DisplayName("관리자는 일일 채팅 보고서를 CSV로 다운로드할 수 있다")
    fun generateDailyChatReport() {
        // Given
        val user = userRepository.findByEmail("member@analytics.com").block()!!
        val thread = threadRepository.save(Thread(userId = user.id.toString())).block()!!
        chatHistoryRepository.save(
            ChatHistory(
                threadId = thread.id!!,
                userId = user.id.toString(),
                userMessage = "Report Test Message",
                assistantMessage = "Report Response",
                createdAt = OffsetDateTime.now()
            )
        ).block()

        // When
        webTestClient.get()
            .uri("/admin/analytics/report")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
            .expectBody(String::class.java)
            .consumeWith { response ->
                val body = response.responseBody!!
                assert(body.contains("User Email,User Name,User Message,Assistant Message,Created At"))
                assert(body.contains("member@analytics.com"))
                assert(body.contains("Report Test Message"))
            }
    }

    @Test
    @DisplayName("일반 회원은 분석 API에 접근할 수 없다")
    fun accessDeniedForMember() {
        webTestClient.get()
            .uri("/admin/analytics/activity")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .exchange()
            .expectStatus().isForbidden

        webTestClient.get()
            .uri("/admin/analytics/report")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $memberToken")
            .exchange()
            .expectStatus().isForbidden
    }
}

