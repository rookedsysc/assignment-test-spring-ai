package com.rokyai.springaipoc.chat.util

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import java.util.UUID

/**
 * Spring Security 관련 유틸리티
 */
object SecurityUtil {
    
    /**
     * 현재 인증된 사용자의 ID를 추출합니다.
     *
     * @return 사용자 ID (UUID), 인증되지 않은 경우 null
     */
    suspend fun getCurrentUserId(): UUID? {
        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication?.principal as? String }
            .map { it?.let { UUID.fromString(it) } }
            .awaitSingleOrNull()
    }

    /**
     * 현재 인증된 사용자의 ID를 추출합니다. (필수)
     *
     * @return 사용자 ID (UUID)
     * @throws IllegalStateException 인증되지 않은 사용자입니다.
     */
    suspend fun requireCurrentUserId(): UUID {
        return getCurrentUserId() 
            ?: throw IllegalStateException("인증되지 않은 사용자입니다.")
    }
}
