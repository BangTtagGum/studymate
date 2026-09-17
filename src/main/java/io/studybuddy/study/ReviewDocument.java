package io.studybuddy.study;

import io.studybuddy.llm.QuestionReview;
import io.studybuddy.llm.ReviewResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * reviews/YYYY-MM-DD.md 렌더링. 헤더에 답변 해시를 남겨 같은 답변을 두 번 채점하지 않는다.
 */
public final class ReviewDocument {

    private static final Pattern HASH = Pattern.compile("<!-- answers-hash: (\\w+) -->");

    private ReviewDocument() {
    }

    public static String extractAnswersHash(String markdown) {
        Matcher m = HASH.matcher(markdown);
        return m.find() ? m.group(1) : null;
    }

    public static String render(String date, String answersHash, ReviewResult result, int answeredCount) {
        int total = result.reviews().stream().mapToInt(QuestionReview::score).sum();
        int max = result.reviews().size() * 10;

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(date).append(" 리뷰  (").append(total).append("/").append(max).append(")\n");
        sb.append("<!-- answers-hash: ").append(answersHash).append(" -->\n\n");
        sb.append("> ").append(result.summary().trim()).append("\n\n");

        for (QuestionReview r : result.reviews()) {
            sb.append("## Q").append(r.number()).append("  —  ").append(r.score()).append("/10\n");
            appendSection(sb, "✅ 맞은 내용", r.correct());
            appendSection(sb, "❌ 틀린 내용", r.incorrect());
            appendSection(sb, "💡 더 알면 좋은 것", r.bonus());
            sb.append("\n");
        }
        if (answeredCount < result.reviews().size()) {
            sb.append("_미답변 질문은 채점하지 않았습니다._\n");
        }
        return sb.toString();
    }

    private static void appendSection(StringBuilder sb, String title, String body) {
        if (body == null || body.isBlank()) return;
        sb.append("- **").append(title).append("**: ").append(body.trim().replace("\n", "\n  ")).append("\n");
    }
}
