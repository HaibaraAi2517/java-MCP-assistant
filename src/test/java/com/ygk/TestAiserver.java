package com.ygk;

import com.ygk.assistant.Assistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestAiserver {
    @Autowired
    private Assistant assistant;
    @Test
    public void testAssistant() {

    String answer = assistant.chat("Hello");
    System.out.println(answer);
    }
}
