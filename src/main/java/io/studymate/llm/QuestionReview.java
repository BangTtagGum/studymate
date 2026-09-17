package io.studymate.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record QuestionReview(
        @JsonPropertyDescription("질문 번호 (1부터)")
        int number,
        @JsonPropertyDescription("0~10 점수. 완전히 맞으면 10, 핵심을 놓쳤으면 5 이하")
        int score,
        @JsonPropertyDescription("답변 중 맞은 내용. 없으면 빈 문자열")
        String correct,
        @JsonPropertyDescription("답변 중 틀리거나 부정확한 내용과 왜 틀렸는지. 없으면 빈 문자열")
        String incorrect,
        @JsonPropertyDescription("추가로 알면 좋은 내용, 관련 개념, 실무 팁")
        String bonus
) {
}
