package com.rokyai.springaipoc.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class OpenAiConfig {

    @Value("\${app.openai.api-key}")
    private lateinit var apiKey: String

    @Value("\${app.openai.chat.model}")
    private lateinit var model: String

    @Value("\${app.openai.chat.temperature:0.7}")
    private var temperature: Double = 0.7

    @Bean(name = ["openAiChatModel"])
    fun openAiChatModel(): ChatModel {
        val openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .build()

        val options = OpenAiChatOptions.builder()
            .model(model)
            .temperature(temperature)
            .build()

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build()
    }

    @Bean(name = ["openAiChatClient"])
    fun openAiChatClient(): ChatClient {
        return ChatClient.create(openAiChatModel())
    }
}
