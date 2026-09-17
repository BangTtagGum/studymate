package io.studybuddy.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record ReviewResult(
        @JsonPropertyDescription("전체 총평 2~3문장")
        String summary,
        List<QuestionReview> reviews
) {
}
