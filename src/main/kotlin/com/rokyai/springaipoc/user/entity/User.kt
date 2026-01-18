package com.rokyai.springaipoc.user.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

/**
 * 사용자 엔티티
 *
 * @property id 사용자 고유 ID (UUID v7)
 * @property email 이메일 주소
 * @property password 암호화된 패스워드
 * @property name 사용자 이름
 * @property role 사용자 권한 (MEMBER, ADMIN)
 * @property createdAt 생성일시 (UTC)
 */
@Table("users")
data class User(
    @Id
    val id: UUID? = null,
    val email: String,
    val password: String,
    val name: String,
    val role: Role = Role.MEMBER,
    val createdAt: Instant = Instant.now()
)
