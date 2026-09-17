package io.studymate.study;

import io.studymate.llm.QuestionReview;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewDocumentTest {

    private static final StudyKey KEY = new StudyKey("jylee", "2026-09-17");

    @Test
    void render_then_parse_round_trips_sections() {
        ReviewDocument doc = ReviewDocument.empty(KEY).merge("총평입니다.\n두 줄", List.of(
                ReviewDocument.Section.of(new QuestionReview(1, 8, "맞음", "", "보너스\n둘째 줄"), "h1"),
                ReviewDocument.Section.of(new QuestionReview(3, 4, "", "틀림", null), "h3")));

        ReviewDocument parsed = ReviewDocument.parse(KEY, doc.render());

        assertThat(parsed.summary()).isEqualTo("총평입니다. 두 줄");
        assertThat(parsed.sections()).containsOnlyKeys(1, 3);
        assertThat(parsed.sections().get(1)).isEqualTo(new ReviewDocument.Section(1, 8, "h1", "맞음", "", "보너스\n둘째 줄"));
        assertThat(parsed.sections().get(3)).isEqualTo(new ReviewDocument.Section(3, 4, "h3", "", "틀림", ""));
        assertThat(parsed.totalScore()).isEqualTo(12);
        assertThat(parsed.maxScore()).isEqualTo(20);
    }

    @Test
    void merge_replaces_section_for_same_number_and_tracks_answer_hash() {
        ReviewDocument first = ReviewDocument.empty(KEY).merge("a", List.of(
                ReviewDocument.Section.of(new QuestionReview(1, 3, "", "틀림", ""), ReviewDocument.hashAnswer("v1"))));
        assertThat(first.isReviewed(1, ReviewDocument.hashAnswer("v1"))).isTrue();
        assertThat(first.isReviewed(1, ReviewDocument.hashAnswer("v2"))).isFalse();

        ReviewDocument second = first.merge("b", List.of(
                ReviewDocument.Section.of(new QuestionReview(1, 9, "맞음", "", ""), ReviewDocument.hashAnswer("v2"))));

        assertThat(second.sections()).hasSize(1);
        assertThat(second.sections().get(1).score()).isEqualTo(9);
        assertThat(second.summary()).isEqualTo("b");
    }
}
