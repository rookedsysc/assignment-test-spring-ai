package com.rokyai.springaipoc.user.service

import com.rokyai.springaipoc.user.dto.UserRegisterRequest
import com.rokyai.springaipoc.user.dto.UserResponse
import com.rokyai.springaipoc.user.entity.User
import com.rokyai.springaipoc.user.exception.DuplicateEmailException
import com.rokyai.springaipoc.user.mapper.UserMapper
import com.rokyai.springaipoc.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * 사용자 관리 서비스
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    /**
     * 회원가입
     *
     * @param request 회원가입 요청 정보
     * @return 생성된 사용자 정보
     * @throws DuplicateEmailException 이미 존재하는 이메일인 경우
     */
    fun register(request: UserRegisterRequest): Mono<UserResponse> {
        return userRepository.existsByEmail(request.email)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(DuplicateEmailException(request.email))
                } else {
                    val user = User(
                        email = request.email,
                        password = passwordEncoder.encode(request.password),
                        name = request.name
                    )
                    userRepository.save(user)
                        .map { UserMapper.toResponse(it) }
                }
            }
    }
}
