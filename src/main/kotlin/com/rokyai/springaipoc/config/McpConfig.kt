package com.rokyai.springaipoc.config

import org.springframework.context.annotation.Configuration

/**
 * MCP(Model Context Protocol) 클라이언트 구성
 *
 * Spring AI MCP Auto-Configuration을 사용합니다.
 * application.yml의 spring.ai.mcp.client.* 설정을 통해 자동으로 구성됩니다.
 *
 * Context7 MCP 서버 연결 설정:
 * - spring.ai.mcp.client.enabled: true
 * - spring.ai.mcp.client.type: ASYNC
 * - spring.ai.mcp.client.toolcallback.enabled: true
 * - spring.ai.mcp.client.streamable-http.connections.context7.url: https://mcp.context7.com
 *
 * 자동으로 생성되는 빈:
 * - McpAsyncClient: Context7 서버와의 비동기 통신 클라이언트
 * - ToolCallbackProvider: MCP 도구를 ChatClient에서 사용할 수 있도록 제공
 */
@Configuration
class McpConfig
