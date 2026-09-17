package io.studybuddy.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record GeneratedQuestion(
        @JsonPropertyDescription("질문 유형: concept(개념 확인) | apply(적용/확장) | clarify(노트의 틀리거나 애매한 부분 짚기)")
        String type,
        @JsonPropertyDescription("학습자에게 던질 질문 본문. 한국어, 한 문단")
        String question
) {
}
