package io.studymate.study;

import io.studymate.config.StudyMateProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 파일 경로 ↔ 날짜 키 변환. notes/2026-09-17.md 의 키는 "2026-09-17".
 */
@Component
public class StudyPaths {

    private static final Pattern MD = Pattern.compile("^([^/]+)/([^/]+)\\.md$");

    private final StudyMateProperties.Study study;

    public StudyPaths(StudyMateProperties properties) {
        this.study = properties.study();
    }

    public Optional<String> noteKey(String path) {
        return keyIfIn(path, study.notesDir());
    }

    public Optional<String> questionKey(String path) {
        return keyIfIn(path, study.questionsDir());
    }

    public String notePath(String key) {
        return study.notesDir() + "/" + key + ".md";
    }

    public String questionPath(String key) {
        return study.questionsDir() + "/" + key + ".md";
    }

    public String reviewPath(String key) {
        return study.reviewsDir() + "/" + key + ".md";
    }

    private static Optional<String> keyIfIn(String path, String dir) {
        Matcher m = MD.matcher(path);
        if (m.matches() && m.group(1).equals(dir)) return Optional.of(m.group(2));
        return Optional.empty();
    }
}
