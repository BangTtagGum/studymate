package io.studybuddy.llm;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

final class PromptLoader {

    private PromptLoader() {
    }

    static String load(String name) {
        try {
            return new ClassPathResource("prompts/" + name).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 로드 실패: " + name, e);
        }
    }
}
