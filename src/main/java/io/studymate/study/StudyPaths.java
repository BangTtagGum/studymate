package io.studymate.study;

import io.studymate.config.StudyMateProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 파일 경로 ↔ {@link StudyKey} 변환. notes/jylee/2026-09-17.md 의 키는 "jylee/2026-09-17".
 */
@Component
public class StudyPaths {

    private static final Pattern MD = Pattern.compile("^([^/]+)/([^/]+)/([^/]+)\\.md$");

    private final StudyMateProperties.Study study;

    public StudyPaths(StudyMateProperties properties) {
        this.study = properties.study();
    }

    public Optional<StudyKey> noteKey(String path) {
        return keyIfIn(path, study.notesDir());
    }

    public Optional<StudyKey> questionKey(String path) {
        return keyIfIn(path, study.questionsDir());
    }

    public String notePath(StudyKey key) {
        return study.notesDir() + "/" + key.member() + "/" + key.date() + ".md";
    }

    public String questionPath(StudyKey key) {
        return study.questionsDir() + "/" + key.member() + "/" + key.date() + ".md";
    }

    public String reviewPath(StudyKey key) {
        return study.reviewsDir() + "/" + key.member() + "/" + key.date() + ".md";
    }

    private static Optional<StudyKey> keyIfIn(String path, String dir) {
        Matcher m = MD.matcher(path);
        if (!m.matches() || !m.group(1).equals(dir)) return Optional.empty();
        return StudyKey.parse(m.group(2) + "/" + m.group(3));
    }
}
