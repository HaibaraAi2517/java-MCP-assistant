package com.ygk.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

//@AiService(
//        wiringMode = EXPLICIT,
//        streamingChatModel = "qwenStreamingChatModel",
//        chatMemoryProvider = "chatMemoryProviderYoke",
//        contentRetriever = "contentRetrieverYokePincone" //配置向量存储
////        toolProvider =  "toolProvider"
//)
public interface YokeAgent {
    @SystemMessage(fromResource = "my-prompt-template.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String userMessage);
}
