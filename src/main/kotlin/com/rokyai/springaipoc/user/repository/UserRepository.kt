package com.rokyai.springaipoc.user.repository

import com.rokyai.springaipoc.user.entity.User
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

/**
 * 사용자 Repository
 */
interface UserRepository : ReactiveCrudRepository<User, UUID> {
    /**
     * 이메일로 사용자 조회
     *
     * @param email 조회할 이메일
     * @return 사용자 정보 (존재하지 않으면 empty Mono)
     */
    fun findByEmail(email: String): Mono<User>

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일
     * @return 존재하면 true, 없으면 false
     */
    fun existsByEmail(email: String): Mono<Boolean>

    /**
     * 특정 시간 이후 생성된 사용자 수 조회
     *
     * @param since 기준 시간
     * @return 사용자 수
     */
    @Query("SELECT COUNT(*) FROM users WHERE created_at >= :since")
    fun countByCreatedAtAfter(since: Instant): Mono<Long>
}
