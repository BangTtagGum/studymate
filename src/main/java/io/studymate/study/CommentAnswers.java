package io.studymate.study;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 이슈 댓글에서 "Q1. 답변" 형태의 답변을 뽑는다. 헤딩(## Q1)·굵게(**Q1**)·구분자(. : ) -) 모두 허용.
 * 다음 Q 헤더 전까지가 해당 질문의 답변이다.
 */
public final class CommentAnswers {

    private static final Pattern HEADER = Pattern.compile(
            "^[ \\t]{0,3}(?:#{1,6}[ \\t]*)?(?:\\*\\*)?[Qq](\\d+)[ \\t]*[.:)\\-]?(?:\\*\\*)?[ \\t]*[.:)\\-]?[ \\t]*",
            Pattern.MULTILINE);

    private CommentAnswers() {
    }

    public static Map<Integer, String> parse(String body) {
        Map<Integer, String> answers = new LinkedHashMap<>();
        if (body == null || body.isBlank()) return answers;

        Matcher m = HEADER.matcher(body);
        int prevNumber = -1;
        int prevStart = -1;
        while (m.find()) {
            if (prevNumber > 0) put(answers, prevNumber, body.substring(prevStart, m.start()));
            prevNumber = Integer.parseInt(m.group(1));
            prevStart = m.end();
        }
        if (prevNumber > 0) put(answers, prevNumber, body.substring(prevStart));
        return answers;
    }

    private static void put(Map<Integer, String> answers, int number, String text) {
        String answer = text.strip();
        if (!answer.isEmpty()) answers.put(number, answer);
    }
}
