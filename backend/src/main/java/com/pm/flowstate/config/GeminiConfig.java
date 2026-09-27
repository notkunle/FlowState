package com.pm.flowstate.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    // only created when GEMINI_API_KEY is set; otherwise GeminiService uses its rule-based fallback
    @Bean
    @ConditionalOnExpression("!'${flowstate.gemini.api-key:}'.isBlank()")
    public Client geminiClient(@Value("${flowstate.gemini.api-key}") String apiKey) {
        return Client.builder().apiKey(apiKey).build();
    }
}
