package com.rokyai.springaipoc.chat.repository

import com.rokyai.springaipoc.chat.entity.ChatHistory
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 채팅 히스토리 레포지토리
 * 채팅 대화 내용을 데이터베이스에 저장하고 조회하는 인터페이스
 */
@Repository
interface ChatHistoryRepository : ReactiveCrudRepository<ChatHistory, UUID> {

    /**
     * 특정 스레드의 모든 채팅 히스토리를 생성일시 오름차순으로 조회합니다.
     *
     * @param threadId 스레드 ID
     * @return 채팅 히스토리 목록
     */
    @Query("SELECT * FROM chat_history WHERE thread_id = :threadId ORDER BY created_at ASC")
    fun findAllByThreadIdOrderByCreatedAtAsc(threadId: UUID): Flux<ChatHistory>

    /**
     * 특정 사용자의 모든 채팅 히스토리를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 채팅 히스토리 목록
     */
    fun findAllByUserId(userId: String): Flux<ChatHistory>

    /**
     * 특정 스레드의 채팅 히스토리를 삭제합니다.
     *
     * @param threadId 스레드 ID
     * @return Flux<Void>
     */
    @Query("DELETE FROM chat_history WHERE thread_id = :threadId")
    fun deleteAllByThreadId(threadId: UUID): Flux<Void>

    /**
     * 특정 시간 이후 생성된 채팅 히스토리 수 조회
     *
     * @param since 기준 시간
     * @return 채팅 히스토리 수
     */
    @Query("SELECT COUNT(*) FROM chat_history WHERE created_at >= :since")
    fun countByCreatedAtAfter(since: OffsetDateTime): Mono<Long>

    /**
     * 특정 시간 이후 생성된 채팅 히스토리 목록 조회
     *
     * @param since 기준 시간
     * @return 채팅 히스토리 목록
     */
    @Query("SELECT * FROM chat_history WHERE created_at >= :since ORDER BY created_at DESC")
    fun findAllByCreatedAtAfter(since: OffsetDateTime): Flux<ChatHistory>
}
