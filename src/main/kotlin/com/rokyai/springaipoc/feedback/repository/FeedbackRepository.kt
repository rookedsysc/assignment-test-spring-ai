package com.rokyai.springaipoc.feedback.repository

import com.rokyai.springaipoc.feedback.entity.Feedback
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 피드백 레포지토리
 * 사용자 피드백 정보를 데이터베이스에 저장하고 조회하는 인터페이스
 */
@Repository
interface FeedbackRepository : ReactiveCrudRepository<Feedback, UUID> {

    /**
     * 사용자 ID로 피드백 목록 조회
     *
     * @param userId 조회할 사용자 ID
     * @param pageable 페이지네이션 및 정렬 정보
     * @return 해당 사용자의 피드백 목록
     */
    fun findByUserId(userId: String, pageable: Pageable): Flux<Feedback>

    /**
     * 긍정/부정 여부로 피드백 목록 조회
     *
     * @param isPositive 긍정(true) 또는 부정(false) 여부
     * @param pageable 페이지네이션 및 정렬 정보
     * @return 조건에 맞는 피드백 목록
     */
    fun findByIsPositive(isPositive: Boolean, pageable: Pageable): Flux<Feedback>

    /**
     * 사용자 ID와 긍정/부정 여부로 피드백 목록 조회
     *
     * @param userId 조회할 사용자 ID
     * @param isPositive 긍정(true) 또는 부정(false) 여부
     * @param pageable 페이지네이션 및 정렬 정보
     * @return 조건에 맞는 피드백 목록
     */
    fun findByUserIdAndIsPositive(userId: String, isPositive: Boolean, pageable: Pageable): Flux<Feedback>

    /**
     * 모든 피드백 목록 조회 (페이지네이션 및 정렬 지원)
     *
     * @param pageable 페이지네이션 및 정렬 정보
     * @return 모든 피드백 목록
     */
    fun findAllBy(pageable: Pageable): Flux<Feedback>

    /**
     * 사용자 ID와 대화 ID로 피드백 존재 여부 확인
     * 동일한 사용자가 같은 대화에 중복 피드백을 생성하는 것을 방지하기 위해 사용
     *
     * @param userId 사용자 ID
     * @param chatId 대화 ID
     * @return 피드백 존재 시 true, 없으면 false
     */
    fun existsByUserIdAndChatId(userId: String, chatId: String): Mono<Boolean>
}
