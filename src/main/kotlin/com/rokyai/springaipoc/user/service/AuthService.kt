package com.rokyai.springaipoc.user.service

import com.rokyai.springaipoc.user.dto.LoginRequest
import com.rokyai.springaipoc.user.dto.TokenResponse
import com.rokyai.springaipoc.user.exception.InvalidPasswordException
import com.rokyai.springaipoc.user.exception.UserNotFoundException
import com.rokyai.springaipoc.user.repository.UserRepository
import com.rokyai.springaipoc.user.util.JwtUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

import com.rokyai.springaipoc.user.repository.LoginHistoryRepository
import com.rokyai.springaipoc.user.entity.LoginHistory

/**
 * 인증 관련 서비스
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val loginHistoryRepository: LoginHistoryRepository
) {
    /**
     * 로그인
     *
     * @param request 로그인 요청 정보
     * @return JWT 토큰 정보
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     * @throws InvalidPasswordException 패스워드가 일치하지 않는 경우
     */
    fun login(request: LoginRequest): Mono<TokenResponse> {
        return userRepository.findByEmail(request.email)
            .switchIfEmpty(Mono.error(UserNotFoundException(request.email)))
            .flatMap { user ->
                if (!passwordEncoder.matches(request.password, user.password)) {
                    Mono.error(InvalidPasswordException())
                } else {
                    val token = jwtUtil.generateToken(
                        userId = user.id!!,
                        email = user.email,
                        role = user.role
                    )
                    
                    loginHistoryRepository.save(LoginHistory(userId = user.id))
                        .thenReturn(
                            TokenResponse(
                                accessToken = token,
                                expiresIn = jwtUtil.getExpirationInSeconds()
                            )
                        )
                }
            }
    }
}
