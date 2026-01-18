package com.rokyai.springaipoc.user.service

import com.rokyai.springaipoc.user.dto.PromoteUserRequest
import com.rokyai.springaipoc.user.dto.UserResponse
import com.rokyai.springaipoc.user.entity.Role
import com.rokyai.springaipoc.user.exception.UserNotFoundException
import com.rokyai.springaipoc.user.mapper.UserMapper
import com.rokyai.springaipoc.user.repository.UserRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * 관리자 관련 서비스
 */
@Service
class AdminService(
    private val userRepository: UserRepository
) {
    /**
     * 회원을 관리자로 승격
     *
     * @param request 승격할 회원 정보
     * @return 승격된 회원 정보
     * @throws UserNotFoundException 사용자를 찾을 수 없는 경우
     */
    fun promoteUserToAdmin(request: PromoteUserRequest): Mono<UserResponse> {
        return userRepository.findById(request.userId)
            .switchIfEmpty(Mono.error(UserNotFoundException(request.userId.toString())))
            .flatMap { user ->
                val updatedUser = user.copy(role = Role.ADMIN)
                userRepository.save(updatedUser)
                    .map { UserMapper.toResponse(it) }
            }
    }
}
