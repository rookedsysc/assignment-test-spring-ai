package com.rokyai.springaipoc.feedback.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * 피드백 엔티티
 * 사용자가 특정 대화에 대해 남긴 피드백 정보를 저장하는 테이블
 */
@Table("feedback")
data class Feedback(
    /**
     * 피드백 고유 ID (UUID)
     */
    @Id
    val id: UUID? = null,

    /**
     * 피드백을 작성한 사용자 ID
     */
    @Column("user_id")
    val userId: String,

    /**
     * 피드백 대상 대화 ID
     */
    @Column("chat_id")
    val chatId: String,

    /**
     * 긍정/부정 피드백 여부
     * true: 긍정, false: 부정
     */
    @Column("is_positive")
    val isPositive: Boolean,

    /**
     * 피드백 상태
     * PENDING: 대기 중, RESOLVED: 해결됨
     */
    @Column("status")
    val status: FeedbackStatus = FeedbackStatus.PENDING,

    /**
     * 피드백 생성 시간 (UTC)
     */
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
)
