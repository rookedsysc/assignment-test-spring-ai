package com.rokyai.springaipoc.feedback.controller

import com.rokyai.springaipoc.feedback.dto.FeedbackCreateRequest
import com.rokyai.springaipoc.feedback.dto.FeedbackResponse
import com.rokyai.springaipoc.feedback.service.FeedbackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 사용자 피드백 API Controller
 * 사용자 피드백의 생성 및 조회 기능을 제공
 */
@RestController
@RequestMapping("/api/v1/feedbacks")
@Tag(name = "Feedback API", description = "사용자 피드백 API")
class FeedbackController(
    private val feedbackService: FeedbackService
) {

    /**
     * 피드백을 생성합니다.
     * 각 사용자는 하나의 대화에 대해 오직 하나의 피드백만 생성할 수 있습니다.
     *
     * @param request 피드백 생성 요청 데이터
     * @return 생성된 피드백 정보
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "피드백 생성",
        description = "특정 대화에 대한 피드백을 생성합니다. 각 사용자는 하나의 대화에 대해 하나의 피드백만 생성할 수 있습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "피드백이 정상적으로 생성되었습니다.",
                content = [Content(schema = Schema(implementation = FeedbackResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청입니다. 필수 값이 누락되었거나 이미 해당 대화에 대한 피드백이 존재합니다.",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류가 발생했습니다.",
                content = [Content()]
            )
        ]
    )
    suspend fun createFeedback(
        @Valid @RequestBody request: FeedbackCreateRequest
    ): FeedbackResponse {
        return feedbackService.createFeedback(request)
    }

    /**
     * 특정 사용자의 피드백 목록을 조회합니다.
     * 각 사용자는 자신이 생성한 피드백만 조회할 수 있습니다.
     *
     * @param userId 조회할 사용자 ID
     * @param isPositive 긍정/부정 필터 (null이면 전체 조회)
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param sort 정렬 기준 (형식: "필드명,방향", 예: "createdAt,desc", 기본값: "createdAt,desc")
     * @return 조건에 맞는 피드백 목록
     */
    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "사용자 피드백 목록 조회",
        description = "특정 사용자가 생성한 피드백 목록을 조회합니다. 긍정/부정 필터링, 페이지네이션, 정렬을 지원합니다."
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
                responseCode = "500",
                description = "서버 내부 오류가 발생했습니다.",
                content = [Content()]
            )
        ]
    )
    suspend fun getUserFeedbacks(
        @PathVariable userId: String,
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
        return feedbackService.getUserFeedbacks(userId, isPositive, pageable)
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
