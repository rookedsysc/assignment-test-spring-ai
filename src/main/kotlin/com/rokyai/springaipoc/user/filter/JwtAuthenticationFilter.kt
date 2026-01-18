package com.rokyai.springaipoc.user.filter

import com.rokyai.springaipoc.user.util.JwtUtil
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * JWT 인증 필터
 *
 * HTTP 요청 헤더에서 JWT 토큰을 추출하고 검증하여 인증 정보를 SecurityContext에 설정합니다.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil
) : WebFilter {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractToken(exchange)

        return if (token != null && jwtUtil.validateToken(token)) {
            val authentication = createAuthentication(token)
            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } else {
            chain.filter(exchange)
        }
    }

    /**
     * HTTP 헤더에서 JWT 토큰 추출
     *
     * @param exchange ServerWebExchange
     * @return JWT 토큰 (Bearer 제거), 없으면 null
     */
    private fun extractToken(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        return if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            authHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    /**
     * JWT 토큰에서 인증 정보 생성
     *
     * @param token JWT 토큰
     * @return UsernamePasswordAuthenticationToken
     */
    private fun createAuthentication(token: String): UsernamePasswordAuthenticationToken {
        val userId = jwtUtil.getUserIdFromToken(token)
        val role = jwtUtil.getRoleFromToken(token)
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

        return UsernamePasswordAuthenticationToken(
            userId.toString(),
            null,
            authorities
        )
    }
}
