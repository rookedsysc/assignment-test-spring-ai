package com.rokyai.springaipoc.user.mapper

import com.rokyai.springaipoc.user.dto.UserResponse
import com.rokyai.springaipoc.user.entity.User

/**
 * User 엔티티와 DTO 간 변환을 담당하는 Mapper
 */
object UserMapper {
    /**
     * User 엔티티를 UserResponse DTO로 변환
     *
     * @param user User 엔티티
     * @return UserResponse DTO
     * @throws IllegalArgumentException user.id가 null인 경우
     */
    fun toResponse(user: User): UserResponse {
        requireNotNull(user.id) { "User ID must not be null" }

        return UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role,
            createdAt = user.createdAt
        )
    }
}
