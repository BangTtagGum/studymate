package io.studymate.study;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 이슈 본문·봇 댓글 렌더링과 마커 판별. 마커는 HTML 주석이라 GitHub 화면에는 보이지 않는다.
 * 봇 댓글 식별을 로그인이 아닌 마커로 하는 이유: PAT 소유자가 스터디 멤버이기도 하면 로그인이 같다.
 */
public final class IssueMarkdown {

    public static final String LABEL = "studymate";

    private static final String QUESTIONS_MARKER = "<!-- studymate:questions key=";
    private static final String REVIEW_MARKER = "<!-- studymate:review -->";
    private static final Pattern KEY = Pattern.compile(Pattern.quote(QUESTIONS_MARKER) + "(\\S+) -->");

    private IssueMarkdown() {
    }

    public static String issueTitle(StudyKey key) {
        return "[" + key.member() + "] " + key.date() + " 질문";
    }

    public static String issueBody(QuestionDocument doc, String questionFilePath) {
        StudyKey key = doc.key();
        StringBuilder sb = new StringBuilder();
        sb.append(QUESTIONS_MARKER).append(key).append(" -->\n");
        sb.append("@").append(key.member()).append(" 님, `").append(doc.sourcePath())
          .append("` 노트를 읽고 만든 이해도 확인 질문입니다.\n\n");
        for (QuestionDocument.Entry e : doc.entries()) {
            sb.append("### Q").append(e.number()).append(". (").append(e.type()).append(") ")
              .append(e.question()).append("\n\n");
        }
        sb.append("---\n");
        sb.append("**답변 방법** — 이 이슈에 댓글로, 질문 번호를 붙여 답합니다. 한 번에 여러 개도 됩니다.\n\n");
        sb.append("```\nQ1. 첫 번째 답변\nQ2. 두 번째 답변\n```\n\n");
        sb.append("답변마다 채점과 더 알아두면 좋은 내용을 댓글로 달아드립니다. ")
          .append("`").append(questionFilePath).append("` 파일의 답변란을 직접 채워 커밋해도 됩니다.\n");
        return sb.toString();
    }

    public static Optional<StudyKey> keyFrom(String issueBody) {
        if (issueBody == null) return Optional.empty();
        Matcher m = KEY.matcher(issueBody);
        return m.find() ? StudyKey.parse(m.group(1)) : Optional.empty();
    }

    public static boolean isBotComment(String commentBody) {
        return commentBody != null && commentBody.contains(REVIEW_MARKER);
    }

    public static String reviewComment(String summary, List<ReviewDocument.Section> sections) {
        StringBuilder sb = new StringBuilder();
        sb.append(REVIEW_MARKER).append("\n");
        for (ReviewDocument.Section sec : sections) {
            sb.append(ReviewDocument.renderSection(sec)).append("\n");
        }
        if (summary != null && !summary.isBlank()) {
            sb.append("> **총평**: ").append(summary.trim().replace("\n", " ")).append("\n");
        }
        return sb.toString();
    }

    public static String completedComment(ReviewDocument review, String reviewFilePath) {
        return REVIEW_MARKER + "\n"
                + "모든 질문 채점이 끝났습니다. **총점 " + review.totalScore() + "/" + review.maxScore() + "**\n\n"
                + "전체 리뷰: `" + reviewFilePath + "`\n";
    }
}
