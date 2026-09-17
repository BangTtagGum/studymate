package io.studymate.study;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 멤버 + 날짜로 하나의 학습 단위를 식별한다. 문자열 표현은 "member/2026-09-17".
 * member 는 GitHub 로그인이며, 이슈 assign·멘션에 그대로 쓰인다.
 */
public record StudyKey(String member, String date) {

    private static final Pattern FORM = Pattern.compile("^([A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)/([^/\\s]+)$");

    public StudyKey {
        if (member == null || member.isBlank()) throw new IllegalArgumentException("member 는 비어 있을 수 없음");
        if (date == null || date.isBlank()) throw new IllegalArgumentException("date 는 비어 있을 수 없음");
    }

    public static Optional<StudyKey> parse(String text) {
        if (text == null) return Optional.empty();
        Matcher m = FORM.matcher(text.trim());
        if (!m.matches()) return Optional.empty();
        return Optional.of(new StudyKey(m.group(1), m.group(2)));
    }

    @Override
    public String toString() {
        return member + "/" + date;
    }
}
