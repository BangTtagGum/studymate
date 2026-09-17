package io.studymate.study;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommentAnswersTest {

    @Test
    void parses_multiple_answers_with_varied_headers() {
        String body = """
                Q1. 격리 수준은 트랜잭션 간 간섭 정도를 정한다.
                두 줄로 씀.

                ## Q2
                REPEATABLE READ 는 같은 행을 다시 읽어도 같다.

                **Q3:** 세 번째
                """;

        Map<Integer, String> answers = CommentAnswers.parse(body);

        assertThat(answers).containsOnlyKeys(1, 2, 3);
        assertThat(answers.get(1)).isEqualTo("격리 수준은 트랜잭션 간 간섭 정도를 정한다.\n두 줄로 씀.");
        assertThat(answers.get(2)).isEqualTo("REPEATABLE READ 는 같은 행을 다시 읽어도 같다.");
        assertThat(answers.get(3)).isEqualTo("세 번째");
    }

    @Test
    void ignores_comments_without_question_headers_or_empty_answers() {
        assertThat(CommentAnswers.parse("그냥 잡담입니다")).isEmpty();
        assertThat(CommentAnswers.parse("Q1.\n\nQ2.")).isEmpty();
        assertThat(CommentAnswers.parse(null)).isEmpty();
    }

    @Test
    void header_must_start_a_line() {
        Map<Integer, String> answers = CommentAnswers.parse("Q1. 본문에 Q2 라는 단어가 있어도 분리되지 않는다");

        assertThat(answers).containsOnlyKeys(1);
    }
}
