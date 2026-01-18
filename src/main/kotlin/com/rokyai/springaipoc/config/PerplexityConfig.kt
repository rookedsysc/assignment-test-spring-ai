package com.rokyai.springaipoc.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class PerplexityConfig {

    @Value("\${app.perplexity.api-key}")
    private lateinit var apiKey: String

    @Value("\${app.perplexity.base-url}")
    private lateinit var baseUrl: String

    @Value("\${app.perplexity.chat.model}")
    private lateinit var model: String

    @Bean(name = ["perplexityRestClientBuilder"])
    fun perplexityRestClientBuilder(): RestClient.Builder {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMinutes(1))
            setReadTimeout(Duration.ofMinutes(10))
        }

        return RestClient.builder()
            .requestFactory(requestFactory)
    }

    @Bean(name = ["perplexityChatModel"])
    fun perplexityChatModel(): ChatModel {
        val openAiApi = OpenAiApi.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .completionsPath("/chat/completions")
            .restClientBuilder(perplexityRestClientBuilder())
            .build()

        val options = OpenAiChatOptions.builder()
            .model(model)
            .build()

        val retryTemplate = RetryTemplate.builder()
            .maxAttempts(2)
            .exponentialBackoff(Duration.ofSeconds(2), 2.0, Duration.ofSeconds(10))
            .build()

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .retryTemplate(retryTemplate)
            .build()
    }

    @Bean(name = ["perplexityChatClient"])
    fun perplexityChatClient(): ChatClient {
        return ChatClient.create(perplexityChatModel())
    }

    /**
     * Perplexity Search API용 RestClient
     *
     * /search 엔드포인트 전용 클라이언트로, 채팅 API보다 검색에 최적화된 기능을 제공합니다.
     * - 최대 20개의 검색 결과 반환
     * - 최신성 필터링 (day/week/month/year)
     * - 언어 및 도메인 필터링
     * - 페이지별 토큰 제어
     *
     * @return Perplexity Search API 호출을 위한 RestClient
     */
    @Bean(name = ["perplexitySearchRestClient"])
    fun perplexitySearchRestClient(): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMinutes(1))
            setReadTimeout(Duration.ofMinutes(10))
        }

        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .defaultHeader("Authorization", "Bearer $apiKey")
            .defaultHeader("Content-Type", "application/json")
            .build()
    }
}
