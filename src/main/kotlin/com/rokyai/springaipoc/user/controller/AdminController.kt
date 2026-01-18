package com.rokyai.springaipoc.user.controller

import com.rokyai.springaipoc.user.dto.PromoteUserRequest
import com.rokyai.springaipoc.user.dto.UserResponse
import com.rokyai.springaipoc.user.service.AdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * 관리자 전용 API Controller
 */
@RestController
@RequestMapping("/admin/users")
@Tag(name = "Admin Management", description = "관리자 전용 사용자 관리 API")
class AdminController(
    private val adminService: AdminService
) {
    /**
     * 회원을 관리자로 승격
     *
     * @param request 승격할 회원 정보 (userId 필수)
     * @return 승격된 회원 정보
     */
    @PostMapping("/promote")
    @Operation(
        summary = "회원 관리자 승격",
        description = "일반 회원을 관리자 권한으로 승격시킵니다. 관리자 권한 필요.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "승격 성공",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (필수값 누락)"
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패 (토큰 없음, 만료, 또는 잘못된 토큰)"
            ),
            ApiResponse(
                responseCode = "403",
                description = "권한 없음 (관리자 권한 필요)"
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음"
            )
        ]
    )
    fun promoteUserToAdmin(@Valid @RequestBody request: PromoteUserRequest): Mono<UserResponse> {
        return adminService.promoteUserToAdmin(request)
    }
}
