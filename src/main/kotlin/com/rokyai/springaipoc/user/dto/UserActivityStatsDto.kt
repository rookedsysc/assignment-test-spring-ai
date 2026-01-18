package com.rokyai.springaipoc.user.dto

data class UserActivityStatsDto(
    val signupCount: Long,
    val loginCount: Long,
    val chatCount: Long
)
