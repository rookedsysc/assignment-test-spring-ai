package com.rokyai.springaipoc.user.controller

import com.rokyai.springaipoc.user.dto.LoginRequest
import com.rokyai.springaipoc.user.dto.TokenResponse
import com.rokyai.springaipoc.user.dto.UserRegisterRequest
import com.rokyai.springaipoc.user.dto.UserResponse
import com.rokyai.springaipoc.user.service.AuthService
import com.rokyai.springaipoc.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * 사용자 인증 관련 API Controller
 */
@RestController
@RequestMapping("/user/auth")
@Tag(name = "User Authentication", description = "사용자 인증 API")
class UserController(
    private val userService: UserService,
    private val authService: AuthService
) {
    /**
     * 회원가입
     *
     * @param request 회원가입 요청 정보 (이메일, 패스워드, 이름 필수)
     * @return 생성된 사용자 정보
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "회원가입",
        description = "새로운 사용자를 등록합니다. 이메일은 고유해야 하며, 패스워드는 8자 이상이어야 합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "회원가입 성공",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (필수값 누락, 형식 오류 등)"
            ),
            ApiResponse(
                responseCode = "409",
                description = "이메일 중복"
            )
        ]
    )
    fun register(@Valid @RequestBody request: UserRegisterRequest): Mono<UserResponse> {
        return userService.register(request)
    }

    /**
     * 로그인
     *
     * @param request 로그인 요청 정보 (이메일, 패스워드 필수)
     * @return JWT 토큰 정보
     */
    @PostMapping("/login")
    @Operation(
        summary = "로그인",
        description = "이메일과 패스워드로 로그인하여 JWT 토큰을 발급받습니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content = [Content(schema = Schema(implementation = TokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (필수값 누락, 형식 오류 등)"
            ),
            ApiResponse(
                responseCode = "401",
                description = "패스워드 불일치"
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음"
            )
        ]
    )
    fun login(@Valid @RequestBody request: LoginRequest): Mono<TokenResponse> {
        return authService.login(request)
    }
}
