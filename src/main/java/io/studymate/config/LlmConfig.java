package io.studymate.config;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import io.studymate.llm.ClaudeStudyLlm;
import io.studymate.llm.DummyStudyLlm;
import io.studymate.llm.StudyLlm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Bean
    StudyLlm studyLlm(StudyMateProperties properties) {
        StudyMateProperties.Claude claude = properties.claude();
        if (!claude.enabled()) {
            log.warn("ANTHROPIC_API_KEY 없음 → DummyStudyLlm 사용 (질문·채점이 더미로 생성됨)");
            return new DummyStudyLlm();
        }
        var client = AnthropicOkHttpClient.builder().apiKey(claude.apiKey()).build();
        log.info("ClaudeStudyLlm 사용 model={}", claude.model());
        return new ClaudeStudyLlm(client, claude.model());
    }
}
