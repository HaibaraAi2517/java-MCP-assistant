package com.ygk.controller;

import com.ygk.assistant.YokeAgent;
import com.ygk.bean.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "Yoke")
@RestController
@RequestMapping("/yoke")
public class YokeController {

    @Autowired
    private YokeAgent yokeAgent;
    @Operation(summary = "对话")
    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm) {
        return yokeAgent.chat(chatForm.getMemoryId(), chatForm.getMessage());
    }
}