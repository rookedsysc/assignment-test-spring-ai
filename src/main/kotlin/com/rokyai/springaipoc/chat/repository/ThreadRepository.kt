package com.rokyai.springaipoc.chat.repository

import com.rokyai.springaipoc.chat.entity.Thread
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

/**
 * 스레드 레포지토리
 *
 * 스레드를 데이터베이스에 저장하고 조회하는 인터페이스
 */
@Repository
interface ThreadRepository : ReactiveCrudRepository<Thread, UUID> {
    
    /**
     * 특정 사용자의 가장 최근 스레드를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 가장 최근 스레드
     */
    @Query("SELECT * FROM thread WHERE user_id = :userId ORDER BY updated_at DESC LIMIT 1")
    fun findLatestByUserId(userId: String): Mono<Thread>
    
    /**
     * 특정 사용자의 모든 스레드를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자의 스레드 목록
     */
    fun findAllByUserId(userId: String): Flux<Thread>
    
    /**
     * 특정 사용자의 스레드를 페이지네이션하여 조회합니다 (생성일시 기준 내림차순).
     *
     * @param userId 사용자 ID
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 스레드 목록
     */
    @Query("SELECT * FROM thread WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findAllByUserIdWithPaginationDesc(userId: String, limit: Int, offset: Int): Flux<Thread>
    
    /**
     * 특정 사용자의 스레드를 페이지네이션하여 조회합니다 (생성일시 기준 오름차순).
     *
     * @param userId 사용자 ID
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 스레드 목록
     */
    @Query("SELECT * FROM thread WHERE user_id = :userId ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    fun findAllByUserIdWithPaginationAsc(userId: String, limit: Int, offset: Int): Flux<Thread>
    
    /**
     * 모든 스레드를 페이지네이션하여 조회합니다 (관리자용, 생성일시 기준 내림차순).
     *
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 스레드 목록
     */
    @Query("SELECT * FROM thread ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun findAllWithPaginationDesc(limit: Int, offset: Int): Flux<Thread>
    
    /**
     * 모든 스레드를 페이지네이션하여 조회합니다 (관리자용, 생성일시 기준 오름차순).
     *
     * @param limit 조회할 개수
     * @param offset 건너뛸 개수
     * @return 스레드 목록
     */
    @Query("SELECT * FROM thread ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    fun findAllWithPaginationAsc(limit: Int, offset: Int): Flux<Thread>
    
    /**
     * 특정 사용자의 특정 스레드가 존재하는지 확인합니다.
     *
     * @param id 스레드 ID
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(*) > 0 FROM thread WHERE id = :id AND user_id = :userId")
    fun existsByIdAndUserId(id: UUID, userId: String): Mono<Boolean>
}
