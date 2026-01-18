package com.rokyai.springaipoc.user.service

import com.rokyai.springaipoc.chat.repository.ChatHistoryRepository
import com.rokyai.springaipoc.user.dto.UserActivityStatsDto
import com.rokyai.springaipoc.user.repository.LoginHistoryRepository
import com.rokyai.springaipoc.user.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class AnalyticsService(
    private val userRepository: UserRepository,
    private val loginHistoryRepository: LoginHistoryRepository,
    private val chatHistoryRepository: ChatHistoryRepository
) {

    fun getDailyActivityStats(): Mono<UserActivityStatsDto> {
        val nowInstant = Instant.now()
        val yesterdayInstant = nowInstant.minus(1, ChronoUnit.DAYS)
        val yesterdayOffset = OffsetDateTime.ofInstant(yesterdayInstant, ZoneOffset.UTC)

        val signupCountMono = userRepository.countByCreatedAtAfter(yesterdayInstant)
        val loginCountMono = loginHistoryRepository.countByCreatedAtAfter(yesterdayInstant)
        val chatCountMono = chatHistoryRepository.countByCreatedAtAfter(yesterdayOffset)

        return Mono.zip(signupCountMono, loginCountMono, chatCountMono)
            .map { tuple ->
                UserActivityStatsDto(
                    signupCount = tuple.t1,
                    loginCount = tuple.t2,
                    chatCount = tuple.t3
                )
            }
    }

    fun generateDailyChatReport(): Mono<String> {
        val nowInstant = Instant.now()
        val yesterdayInstant = nowInstant.minus(1, ChronoUnit.DAYS)
        val yesterdayOffset = OffsetDateTime.ofInstant(yesterdayInstant, ZoneOffset.UTC)

        return chatHistoryRepository.findAllByCreatedAtAfter(yesterdayOffset)
            .flatMap { chat ->
                // userId가 UUID 형식이 아닌 경우 처리 필요할 수 있음
                // 여기서는 UUID라고 가정하고 처리
                val userId = try {
                    UUID.fromString(chat.userId)
                } catch (e: IllegalArgumentException) {
                    null
                }

                if (userId != null) {
                    userRepository.findById(userId)
                        .map { user ->
                            Triple(chat, user.email, user.name)
                        }
                        .defaultIfEmpty(Triple(chat, "Unknown", "Unknown"))
                } else {
                    Mono.just(Triple(chat, "Unknown", "Unknown"))
                }
            }
            .collectList()
            .map { list ->
                val header = "User Email,User Name,User Message,Assistant Message,Created At"
                val rows = list.map { (chat, email, name) ->
                    val escapedUserMessage = escapeCsv(chat.userMessage)
                    val escapedAssistantMessage = escapeCsv(chat.assistantMessage)
                    "$email,$name,$escapedUserMessage,$escapedAssistantMessage,${chat.createdAt}"
                }
                (listOf(header) + rows).joinToString("\n")
            }
    }

    private fun escapeCsv(data: String): String {
        var escapedData = data.replace("\"", "\"\"")
        if (escapedData.contains(",") || escapedData.contains("\n") || escapedData.contains("\"")) {
            escapedData = "\"$escapedData\""
        }
        return escapedData
    }
}

