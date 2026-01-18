package com.rokyai.springaipoc.user.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

/**
 * 로그인 히스토리 엔티티
 */
@Table("login_history")
data class LoginHistory(
    @Id
    val id: UUID? = null,
    
    @Column("user_id")
    val userId: UUID,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
