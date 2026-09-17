package io.studymate.study;

import io.studymate.llm.GeneratedQuestion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionDocumentTest {

    private static final StudyKey KEY = new StudyKey("jylee", "2026-09-17");

    @Test
    void render_then_parse_round_trips_unanswered_questions() {
        QuestionDocument doc = QuestionDocument.fromGenerated(KEY, "notes/jylee/2026-09-17.md", "abc123",
                List.of(new GeneratedQuestion("concept", "트랜잭션 격리 수준을 설명하세요."),
                        new GeneratedQuestion("apply", "REPEATABLE READ 가 필요한 상황은?")))
                .withIssueNumber(12);

        QuestionDocument parsed = QuestionDocument.parse(KEY, doc.render());

        assertThat(parsed.sourcePath()).isEqualTo("notes/jylee/2026-09-17.md");
        assertThat(parsed.sourceSha()).isEqualTo("abc123");
        assertThat(parsed.issueNumber()).isEqualTo(12);
        assertThat(parsed.entries()).hasSize(2);
        assertThat(parsed.entries().get(0).type()).isEqualTo("concept");
        assertThat(parsed.entries().get(1).question()).isEqualTo("REPEATABLE READ 가 필요한 상황은?");
        assertThat(parsed.answeredEntries()).isEmpty();
    }

    @Test
    void parse_detects_filled_answers_only() {
        String md = """
                # jylee 2026-09-17 질문
                > 원본: notes/jylee/2026-09-17.md @ abc123

                ## Q1. (concept) 첫 번째 질문
                <!-- answer -->
                여러 줄로
                작성한 답변
                <!-- /answer -->

                ## Q2. (apply) 두 번째 질문
                <!-- answer -->

                <!-- /answer -->
                """;

        QuestionDocument parsed = QuestionDocument.parse(KEY, md);

        assertThat(parsed.entries()).hasSize(2);
        assertThat(parsed.issueNumber()).isNull();
        assertThat(parsed.answeredEntries()).hasSize(1);
        assertThat(parsed.answeredEntries().get(0).answer()).isEqualTo("여러 줄로\n작성한 답변");
    }

    @Test
    void withAnswers_overrides_known_numbers_and_ignores_unknown() {
        QuestionDocument doc = QuestionDocument.fromGenerated(KEY, "notes/jylee/2026-09-17.md", "abc123",
                List.of(new GeneratedQuestion("concept", "q1"), new GeneratedQuestion("apply", "q2")));

        QuestionDocument updated = doc.withAnswers(Map.of(2, "  두 번째 답 \n", 9, "없는 번호"));

        assertThat(updated.entries().get(0).answered()).isFalse();
        assertThat(updated.entries().get(1).answer()).isEqualTo("두 번째 답");
        assertThat(updated.hasEntry(9)).isFalse();
        assertThat(QuestionDocument.parse(KEY, updated.render())).isEqualTo(updated);
    }
}
