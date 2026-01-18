package com.rokyai.springaipoc.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class GeminiConfig {

    @Value("\${app.gemini.api-key}")
    private lateinit var apiKey: String

    @Value("\${app.gemini.base-url}")
    private lateinit var baseUrl: String

    /**
     * Gemini Deep Research API 호출을 위한 RestClient를 생성합니다.
     *
     * Deep Research는 60분까지 소요될 수 있으므로 타임아웃을 길게 설정합니다.
     *
     * @return Gemini API 호출을 위한 RestClient 인스턴스
     */
    @Bean(name = ["geminiDeepResearchRestClient"])
    fun geminiDeepResearchRestClient(): RestClient {
        val requestFactory = JdkClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofMinutes(65))  // Deep Research 최대 시간(60분) + 버퍼
        }

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("x-goog-api-key", apiKey)
            .build()
    }
}
