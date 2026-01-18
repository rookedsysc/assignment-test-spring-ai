package com.rokyai.springaipoc.chat.enums

import org.springframework.ai.chat.client.ChatClient

/**
 * AI 채팅 제공자 및 모델 정의
 *
 * 지원하는 AI 제공자와 해당 모델을 정의하고, Factory Method Pattern을 통해
 * 적절한 ChatClient를 반환합니다.
 */
enum class ChatProvider(
    /**
     * 제공자 이름
     */
    val providerName: String,
    
    /**
     * 모델 이름
     */
    val modelName: String,
    
    /**
     * Spring Bean 이름 (ChatClient)
     */
    val beanName: String
) {
    /**
     * OpenAI GPT-4o
     */
    OPENAI_GPT4O("OpenAI", "gpt-4o", "openAiChatClient"),
    
    /**
     * OpenAI GPT-4o-mini
     */
    OPENAI_GPT4O_MINI("OpenAI", "gpt-4o-mini", "openAiChatClient"),
    
    /**
     * OpenAI GPT-4-turbo
     */
    OPENAI_GPT4_TURBO("OpenAI", "gpt-4-turbo", "openAiChatClient"),
    
    /**
     * Perplexity Sonar
     */
    PERPLEXITY_SONAR("Perplexity", "sonar", "perplexityChatClient"),
    
    /**
     * Perplexity Sonar Pro
     */
    PERPLEXITY_SONAR_PRO("Perplexity", "sonar-pro", "perplexityChatClient");
    
    companion object {
        /**
         * 기본 ChatProvider를 반환합니다.
         *
         * @return OpenAI GPT-4o
         */
        fun default(): ChatProvider = OPENAI_GPT4O
    }
}
