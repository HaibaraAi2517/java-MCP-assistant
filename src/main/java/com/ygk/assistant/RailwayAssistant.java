package com.ygk.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * 火车票系统AI助手
 * 集成MCP工具，提供火车票查询和预订功能
 */
@AiService(
        wiringMode = EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProviderYoke"
        // 注意：toolProvider属性在当前版本的langchain4j中可能不支持
        // 需要通过其他方式集成MCP工具
)
public interface RailwayAssistant {
    
    /**
     * 与用户进行对话，可以处理火车票相关的查询和预订
     * 
     * @param memoryId 记忆ID，用于维护对话上下文
     * @param userMessage 用户消息
     * @return 流式响应
     */
    @SystemMessage("你是一个专业的火车票查询和预订助手。你可以帮助用户查询火车票余量、预订火车票等。请用中文回复用户。")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String userMessage);
}
