package io.studybuddy.llm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DummyStudyLlmTest {

    @Test
    void builds_questions_from_headings() {
        String note = """
                # DB 트랜잭션
                ## 격리 수준
                내용
                ## 데드락
                내용
                """;

        GeneratedQuestions q = new DummyStudyLlm().generateQuestions(note, 4);

        assertThat(q.questions()).hasSize(4);
        assertThat(q.questions().get(1).question()).contains("격리 수준");
        assertThat(q.questions().get(3).type()).isEqualTo("apply");
    }

    @Test
    void review_returns_one_entry_per_answer() {
        ReviewResult r = new DummyStudyLlm().review("note",
                List.of(new AnsweredQuestion(1, "concept", "q", "a"),
                        new AnsweredQuestion(3, "apply", "q", "aaa")));

        assertThat(r.reviews()).extracting(QuestionReview::number).containsExactly(1, 3);
    }
}
