package com.rokyai.springaipoc.chat.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * 스레드 엔티티
 *
 * 대화 목록을 그룹화하는 단위입니다.
 * 같은 스레드에 속한 대화 목록은 AI에게 컨텍스트로 전송됩니다.
 */
@Table("thread")
data class Thread(
    /**
     * 스레드 고유 ID (UUID)
     */
    @Id
    val id: UUID? = null,

    /**
     * 스레드를 소유한 사용자 ID
     */
    @Column("user_id")
    val userId: String,

    /**
     * 스레드 생성 시간 (UTC)
     */
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),

    /**
     * 스레드의 마지막 업데이트 시간 (UTC)
     *
     * 새로운 대화가 추가될 때마다 업데이트됩니다.
     */
    @Column("updated_at")
    val updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
)
