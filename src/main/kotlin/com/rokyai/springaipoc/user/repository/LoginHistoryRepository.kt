package com.rokyai.springaipoc.user.repository

import com.rokyai.springaipoc.user.entity.LoginHistory
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
interface LoginHistoryRepository : ReactiveCrudRepository<LoginHistory, UUID> {
    
    @Query("SELECT COUNT(*) FROM login_history WHERE created_at >= :since")
    fun countByCreatedAtAfter(since: Instant): Mono<Long>
}
