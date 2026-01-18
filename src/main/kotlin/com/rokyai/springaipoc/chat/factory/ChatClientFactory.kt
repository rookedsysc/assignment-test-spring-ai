package com.rokyai.springaipoc.chat.factory

import com.rokyai.springaipoc.chat.enums.ChatProvider
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * ChatProvider enum에 따라 적절한 ChatClient를 반환하는 Factory
 *
 * @property openAiChatClient OpenAI용 ChatClient
 * @property perplexityChatClient Perplexity용 ChatClient
 */
@Component
class ChatClientFactory(
    @Qualifier("openAiChatClient") private val openAiChatClient: ChatClient,
    @Qualifier("perplexityChatClient") private val perplexityChatClient: ChatClient
) {
    /**
     * ChatProvider에 해당하는 ChatClient를 반환합니다.
     *
     * @param provider AI 제공자 및 모델 정보를 담은 enum
     * @return 해당 provider에 맞는 ChatClient
     */
    fun getClient(provider: ChatProvider): ChatClient {
        return when (provider.beanName) {
            "openAiChatClient" -> openAiChatClient
            "perplexityChatClient" -> perplexityChatClient
            else -> throw IllegalArgumentException("지원하지 않는 ChatClient입니다. beanName: ${provider.beanName}")
        }
    }

    /**
     * ChatProvider에 해당하는 ChatOptions를 반환합니다.
     *
     * @param provider AI 제공자 및 모델 정보를 담은 enum
     * @return 해당 provider에 맞는 ChatOptions
     */
    fun getOptions(provider: ChatProvider): ChatOptions {
        return OpenAiChatOptions.builder()
            .model(provider.modelName)
            .build()
    }
}
