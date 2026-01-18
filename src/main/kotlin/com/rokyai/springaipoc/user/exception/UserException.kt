package com.rokyai.springaipoc.user.exception

/**
 * 이메일 중복 예외
 */
class DuplicateEmailException(email: String) : RuntimeException("이미 존재하는 이메일입니다: $email")

/**
 * 사용자를 찾을 수 없는 예외
 */
class UserNotFoundException(email: String) : RuntimeException("사용자를 찾을 수 없습니다: $email")

/**
 * 잘못된 패스워드 예외
 */
class InvalidPasswordException : RuntimeException("패스워드가 일치하지 않습니다")
