package io.studymate.study;

import io.studymate.config.StudyMateProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudyPathsTest {

    private final StudyPaths paths = new StudyPaths(new StudyMateProperties(
            new StudyMateProperties.Github("o", "r", "main", "t", null, null, null, null),
            new StudyMateProperties.Claude(null, null),
            new StudyMateProperties.Study(null, null, null, 0)));

    @Test
    void maps_member_paths_to_keys_and_back() {
        StudyKey key = new StudyKey("jylee", "2026-09-17");

        assertThat(paths.noteKey("notes/jylee/2026-09-17.md")).contains(key);
        assertThat(paths.questionKey("questions/jylee/2026-09-17.md")).contains(key);
        assertThat(paths.notePath(key)).isEqualTo("notes/jylee/2026-09-17.md");
        assertThat(paths.questionPath(key)).isEqualTo("questions/jylee/2026-09-17.md");
        assertThat(paths.reviewPath(key)).isEqualTo("reviews/jylee/2026-09-17.md");
    }

    @Test
    void rejects_paths_outside_the_member_layout() {
        assertThat(paths.noteKey("notes/2026-09-17.md")).isEmpty();
        assertThat(paths.noteKey("notes/jylee/sub/2026-09-17.md")).isEmpty();
        assertThat(paths.noteKey("questions/jylee/2026-09-17.md")).isEmpty();
        assertThat(paths.noteKey("notes/-bad-/2026-09-17.md")).isEmpty();
    }

    @Test
    void study_key_string_form_round_trips() {
        assertThat(StudyKey.parse("jylee/2026-09-17")).contains(new StudyKey("jylee", "2026-09-17"));
        assertThat(StudyKey.parse("jylee")).isEmpty();
        assertThat(new StudyKey("jylee", "2026-09-17").toString()).isEqualTo("jylee/2026-09-17");
    }
}
