package io.studymate.study;

import io.studymate.llm.GeneratedQuestion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionDocumentTest {

    @Test
    void render_then_parse_round_trips_unanswered_questions() {
        QuestionDocument doc = QuestionDocument.fromGenerated("2026-09-17", "notes/2026-09-17.md", "abc123",
                List.of(new GeneratedQuestion("concept", "트랜잭션 격리 수준을 설명하세요."),
                        new GeneratedQuestion("apply", "REPEATABLE READ 가 필요한 상황은?")));

        QuestionDocument parsed = QuestionDocument.parse("2026-09-17", doc.render());

        assertThat(parsed.sourcePath()).isEqualTo("notes/2026-09-17.md");
        assertThat(parsed.sourceSha()).isEqualTo("abc123");
        assertThat(parsed.entries()).hasSize(2);
        assertThat(parsed.entries().get(0).type()).isEqualTo("concept");
        assertThat(parsed.entries().get(1).question()).isEqualTo("REPEATABLE READ 가 필요한 상황은?");
        assertThat(parsed.answeredEntries()).isEmpty();
    }

    @Test
    void parse_detects_filled_answers_only() {
        String md = """
                # 2026-09-17 질문
                > 원본: notes/2026-09-17.md @ abc123

                ## Q1. (concept) 첫 번째 질문
                <!-- answer -->
                여러 줄로
                작성한 답변
                <!-- /answer -->

                ## Q2. (apply) 두 번째 질문
                <!-- answer -->

                <!-- /answer -->
                """;

        QuestionDocument parsed = QuestionDocument.parse("2026-09-17", md);

        assertThat(parsed.entries()).hasSize(2);
        assertThat(parsed.answeredEntries()).hasSize(1);
        assertThat(parsed.answeredEntries().get(0).answer()).isEqualTo("여러 줄로\n작성한 답변");
    }
}
