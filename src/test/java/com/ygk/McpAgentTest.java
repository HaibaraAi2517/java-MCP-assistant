package com.ygk;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class McpAgentTest {

    interface ToolAwareAssistant {
        String chat(String userMessage);
    }

    @Autowired
    private ChatLanguageModel ollamaChatModel1;

    @Autowired
    private ToolProvider toolProvider; // from McpClientConfig

    @Test
    void should_invoke_mcp_tools() {
        ToolAwareAssistant assistant = AiServices.builder(ToolAwareAssistant.class)
                .chatLanguageModel(ollamaChatModel1)
                .toolProvider(toolProvider)
                .build();

        String reply = assistant.chat("请帮我查询从北京到上海的火车票余量，并预订2张票。");
        System.out.println("Assistant reply: " + reply);
    }
}


