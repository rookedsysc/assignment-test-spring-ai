package com.rokyai.springaipoc.feedback.controller

import com.rokyai.springaipoc.feedback.dto.FeedbackResponse
import com.rokyai.springaipoc.feedback.dto.FeedbackUpdateStatusRequest
import com.rokyai.springaipoc.feedback.service.FeedbackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * 관리자 전용 피드백 관리 API Controller
 * 모든 사용자의 피드백 조회 및 상태 관리 기능을 제공
 */
@RestController
@RequestMapping("/admin/feedbacks")
@Tag(name = "Admin Feedback API", description = "관리자 전용 피드백 관리 API")
class AdminFeedbackController(
    private val feedbackService: FeedbackService
) {

    /**
     * 모든 피드백 목록을 조회합니다.
     * 관리자는 모든 사용자의 피드백을 조회할 수 있습니다.
     *
     * @param isPositive 긍정/부정 필터 (null이면 전체 조회)
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param sort 정렬 기준 (형식: "필드명,방향", 예: "createdAt,desc", 기본값: "createdAt,desc")
     * @return 조건에 맞는 전체 피드백 목록
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "전체 피드백 목록 조회",
        description = "모든 사용자의 피드백 목록을 조회합니다. 관리자만 접근 가능하며, 긍정/부정 필터링, 페이지네이션, 정렬을 지원합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "피드백 목록을 정상적으로 조회했습니다.",
                content = [Content(array = ArraySchema(schema = Schema(implementation = FeedbackResponse::class)))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청입니다. 페이지 번호나 크기가 유효하지 않습니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패 (토큰 없음, 만료, 또는 잘못된 토큰)",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "403",
                description = "권한 없음 (관리자 권한 필요)",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류가 발생했습니다.",
                content = [Content()]
            )
        ]
    )
    suspend fun getAllFeedbacks(
        @Parameter(description = "긍정(true) 또는 부정(false) 필터 (null이면 전체 조회)")
        @RequestParam(required = false) isPositive: Boolean?,
        @Parameter(description = "페이지 번호 (0부터 시작)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "정렬 기준 (형식: 필드명,방향)")
        @RequestParam(defaultValue = "createdAt,desc") sort: String
    ): List<FeedbackResponse> {
        val pageable = createPageable(page, size, sort)
        return feedbackService.getAllFeedbacks(isPositive, pageable)
    }

    /**
     * 피드백의 상태를 변경합니다.
     * 관리자만 피드백의 상태를 변경할 수 있습니다.
     *
     * @param feedbackId 변경할 피드백 ID
     * @param request 변경할 상태 정보
     * @return 상태가 변경된 피드백 정보
     */
    @PatchMapping("/{feedbackId}/status")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "피드백 상태 변경",
        description = "피드백의 상태를 변경합니다. 관리자만 접근 가능합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "피드백 상태가 정상적으로 변경되었습니다.",
                content = [Content(schema = Schema(implementation = FeedbackResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청입니다. 상태 값이 유효하지 않습니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패 (토큰 없음, 만료, 또는 잘못된 토큰)",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "403",
                description = "권한 없음 (관리자 권한 필요)",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "404",
                description = "피드백을 찾을 수 없습니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류가 발생했습니다.",
                content = [Content()]
            )
        ]
    )
    suspend fun updateFeedbackStatus(
        @PathVariable feedbackId: UUID,
        @Valid @RequestBody request: FeedbackUpdateStatusRequest
    ): FeedbackResponse {
        return feedbackService.updateFeedbackStatus(feedbackId, request.status)
    }

    /**
     * 페이지네이션과 정렬 정보를 생성합니다.
     * sort 파라미터를 파싱하여 PageRequest 객체를 생성합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 기준 (형식: "필드명,방향")
     * @return PageRequest 객체
     */
    private fun createPageable(page: Int, size: Int, sort: String): PageRequest {
        val sortParams = sort.split(",")
        val sortField = sortParams.getOrNull(0) ?: "createdAt"
        val sortDirection = sortParams.getOrNull(1)?.let {
            if (it.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        } ?: Sort.Direction.DESC

        return PageRequest.of(page, size, Sort.by(sortDirection, sortField))
    }
}
