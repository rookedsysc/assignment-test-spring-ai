package com.rokyai.springaipoc.feedback.entity

/**
 * 피드백 상태
 * 피드백의 처리 상태를 나타내는 열거형
 */
enum class FeedbackStatus {
    /**
     * 대기 중 상태
     * 피드백이 생성되었으나 아직 처리되지 않은 상태
     */
    PENDING,

    /**
     * 해결됨 상태
     * 피드백이 검토 및 처리 완료된 상태
     */
    RESOLVED
}
