package com.rokyai.springaipoc.user.util

import com.rokyai.springaipoc.user.entity.Role
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT 토큰 생성 및 검증 유틸리티
 */
@Component
class JwtUtil(
    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.expiration}")
    private val expiration: Long
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * JWT 토큰 생성
     *
     * @param userId 사용자 ID
     * @param email 이메일
     * @param role 사용자 권한
     * @return 생성된 JWT 토큰
     */
    fun generateToken(userId: UUID, email: String, role: Role): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role.name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    fun getUserIdFromToken(token: String): UUID {
        val claims = getAllClaimsFromToken(token)
        return UUID.fromString(claims.subject)
    }

    /**
     * JWT 토큰에서 이메일 추출
     *
     * @param token JWT 토큰
     * @return 이메일
     */
    fun getEmailFromToken(token: String): String {
        val claims = getAllClaimsFromToken(token)
        return claims["email"] as String
    }

    /**
     * JWT 토큰에서 권한 추출
     *
     * @param token JWT 토큰
     * @return 사용자 권한
     */
    fun getRoleFromToken(token: String): Role {
        val claims = getAllClaimsFromToken(token)
        val roleString = claims["role"] as String
        return Role.valueOf(roleString)
    }

    /**
     * JWT 토큰 유효성 검증
     *
     * @param token JWT 토큰
     * @return 유효하면 true, 그렇지 않으면 false
     */
    fun validateToken(token: String): Boolean {
        return try {
            getAllClaimsFromToken(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * JWT 토큰에서 모든 클레임 추출
     *
     * @param token JWT 토큰
     * @return 클레임
     */
    private fun getAllClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * 토큰 만료 시간 반환 (초)
     *
     * @return 만료 시간 (초)
     */
    fun getExpirationInSeconds(): Long {
        return expiration / 1000
    }
}
