package com.rokyai.springaipoc.user.controller

import com.rokyai.springaipoc.user.dto.UserActivityStatsDto
import com.rokyai.springaipoc.user.service.AnalyticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/admin/analytics")
@Tag(name = "Admin Analytics", description = "관리자 전용 분석 및 보고서 API")
class AdminAnalyticsController(
    private val analyticsService: AnalyticsService
) {

    @GetMapping("/activity")
    @Operation(
        summary = "사용자 활동 기록 조회",
        description = "최근 24시간 동안의 회원가입, 로그인, 대화 생성 수를 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = UserActivityStatsDto::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "권한 없음"
            )
        ]
    )
    fun getUserActivityStats(): Mono<UserActivityStatsDto> {
        return analyticsService.getDailyActivityStats()
    }

    @GetMapping("/report")
    @Operation(
        summary = "일일 대화 보고서 생성",
        description = "최근 24시간 동안의 모든 대화 목록을 CSV 형태로 다운로드합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "보고서 생성 성공",
                content = [Content(mediaType = "text/csv")]
            ),
            ApiResponse(
                responseCode = "403",
                description = "권한 없음"
            )
        ]
    )
    fun generateDailyReport(): Mono<ResponseEntity<Resource>> {
        return analyticsService.generateDailyChatReport()
            .map {
                val resource = ByteArrayResource(it.toByteArray(Charsets.UTF_8))
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily_chat_report.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                    .body(resource)
            }
    }
}
