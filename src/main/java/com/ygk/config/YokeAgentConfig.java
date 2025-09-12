package com.ygk.config;

import com.ygk.assistant.YokeAgent;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class YokeAgentConfig {


    @Bean
    @Lazy
    public YokeAgent yokeAgent(
            @Qualifier("qwenStreamingChatModel") StreamingChatLanguageModel qwenStreamingChatModel,
            @Qualifier("chatMemoryProviderYoke") ChatMemoryProvider chatMemoryProviderYoke,
            ContentRetriever contentRetrieverYokePincone,
            @Lazy ToolProvider toolProvider
    ) {
        return AiServices.builder(YokeAgent.class)
                .streamingChatLanguageModel(qwenStreamingChatModel)
                .chatMemoryProvider(chatMemoryProviderYoke)
                .contentRetriever(contentRetrieverYokePincone)
                .toolProvider(toolProvider)
                .build();
    }


}