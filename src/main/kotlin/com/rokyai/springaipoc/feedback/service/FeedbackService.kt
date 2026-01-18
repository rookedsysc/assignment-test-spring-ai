package com.rokyai.springaipoc.feedback.service

import com.rokyai.springaipoc.feedback.dto.FeedbackCreateRequest
import com.rokyai.springaipoc.feedback.dto.FeedbackResponse
import com.rokyai.springaipoc.feedback.entity.Feedback
import com.rokyai.springaipoc.feedback.entity.FeedbackStatus
import com.rokyai.springaipoc.feedback.repository.FeedbackRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * 피드백 관리 서비스
 * 사용자 피드백의 생성, 조회, 상태 변경 등의 비즈니스 로직을 처리
 */
@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository
) {

    /**
     * 피드백을 생성합니다.
     * 동일한 사용자가 같은 대화에 대해 중복으로 피드백을 생성할 수 없습니다.
     *
     * @param request 피드백 생성 요청 데이터
     * @return 생성된 피드백 정보
     * @throws IllegalArgumentException 동일한 사용자가 같은 대화에 이미 피드백을 생성한 경우
     */
    suspend fun createFeedback(request: FeedbackCreateRequest): FeedbackResponse {
        val exists = feedbackRepository.existsByUserIdAndChatId(request.userId, request.chatId)
            .awaitSingle()

        if (exists) {
            throw IllegalArgumentException("이미 해당 대화에 대한 피드백이 존재합니다. (userId: ${request.userId}, chatId: ${request.chatId})")
        }

        val feedback = Feedback(
            userId = request.userId,
            chatId = request.chatId,
            isPositive = request.isPositive,
            status = FeedbackStatus.PENDING,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        )

        val savedFeedback = feedbackRepository.save(feedback).awaitSingle()
        return FeedbackResponse.from(savedFeedback)
    }

    /**
     * 특정 사용자의 피드백 목록을 조회합니다.
     * 긍정/부정 필터링이 가능하며, 페이지네이션과 정렬을 지원합니다.
     *
     * @param userId 조회할 사용자 ID
     * @param isPositive 긍정/부정 필터 (null이면 전체 조회)
     * @param pageable 페이지네이션 및 정렬 정보
     * @return 조건에 맞는 피드백 목록
     */
    suspend fun getUserFeedbacks(
        userId: String,
        isPositive: Boolean?,
        pageable: Pageable
    ): List<FeedbackResponse> {
        val feedbacksFlux = if (isPositive != null) {
            feedbackRepository.findByUserIdAndIsPositive(userId, isPositive, pageable)
        } else {
            feedbackRepository.findByUserId(userId, pageable)
        }

        return feedbacksFlux
            .map { FeedbackResponse.from(it) }
            .collectList()
            .awaitSingle()
    }

    /**
     * 모든 피드백 목록을 조회합니다. (관리자용)
     * 긍정/부정 필터링이 가능하며, 페이지네이션과 정렬을 지원합니다.
     *
     * @param isPositive 긍정/부정 필터 (null이면 전체 조회)
     * @param pageable 페이지네이션 및 정렬 정보
     * @return 조건에 맞는 전체 피드백 목록
     */
    suspend fun getAllFeedbacks(
        isPositive: Boolean?,
        pageable: Pageable
    ): List<FeedbackResponse> {
        val feedbacksFlux = if (isPositive != null) {
            feedbackRepository.findByIsPositive(isPositive, pageable)
        } else {
            feedbackRepository.findAllBy(pageable)
        }

        return feedbacksFlux
            .map { FeedbackResponse.from(it) }
            .collectList()
            .awaitSingle()
    }

    /**
     * 피드백의 상태를 변경합니다. (관리자용)
     *
     * @param feedbackId 변경할 피드백 ID
     * @param newStatus 새로운 상태
     * @return 상태가 변경된 피드백 정보
     * @throws NoSuchElementException 피드백을 찾을 수 없는 경우
     */
    suspend fun updateFeedbackStatus(
        feedbackId: UUID,
        newStatus: FeedbackStatus
    ): FeedbackResponse {
        val feedback = feedbackRepository.findById(feedbackId)
            .awaitSingleOrNull()
            ?: throw NoSuchElementException("피드백을 찾을 수 없습니다. (feedbackId: $feedbackId)")

        val updatedFeedback = feedback.copy(status = newStatus)
        val savedFeedback = feedbackRepository.save(updatedFeedback).awaitSingle()

        return FeedbackResponse.from(savedFeedback)
    }
}
