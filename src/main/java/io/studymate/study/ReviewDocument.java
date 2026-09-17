package io.studymate.study;

import io.studymate.llm.QuestionReview;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * reviews/&lt;member&gt;/&lt;date&gt;.md 의 구조. 질문별 최신 리뷰를 보관하며,
 * 각 섹션에 답변 해시를 남겨 같은 답변을 두 번 채점하지 않는다.
 */
public record ReviewDocument(StudyKey key, String summary, Map<Integer, Section> sections) {

    private static final String CORRECT = "✅ 맞은 내용";
    private static final String INCORRECT = "❌ 틀린 내용";
    private static final String BONUS = "💡 더 알면 좋은 것";

    private static final Pattern SUMMARY = Pattern.compile("^> 총평: (.*)$", Pattern.MULTILINE);
    private static final Pattern SECTION = Pattern.compile(
            "^## Q(\\d+)\\s+—\\s+(\\d+)/10\\s*$\\n<!-- answer-hash: (\\w+) -->\\n(.*?)(?=^## Q|\\z)",
            Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern BULLET = Pattern.compile(
            "^- \\*\\*(" + CORRECT + "|" + INCORRECT + "|" + BONUS + ")\\*\\*: (.*?)(?=^- \\*\\*|\\z)",
            Pattern.MULTILINE | Pattern.DOTALL);

    public record Section(int number, int score, String answerHash,
                          String correct, String incorrect, String bonus) {
        public static Section of(QuestionReview r, String answerHash) {
            return new Section(r.number(), r.score(), answerHash,
                    nullToEmpty(r.correct()), nullToEmpty(r.incorrect()), nullToEmpty(r.bonus()));
        }
    }

    public static ReviewDocument empty(StudyKey key) {
        return new ReviewDocument(key, "", new TreeMap<>());
    }

    public static ReviewDocument parse(StudyKey key, String markdown) {
        String summary = "";
        Matcher s = SUMMARY.matcher(markdown);
        if (s.find()) summary = s.group(1).trim();

        Map<Integer, Section> sections = new TreeMap<>();
        Matcher m = SECTION.matcher(markdown);
        while (m.find()) {
            int number = Integer.parseInt(m.group(1));
            Map<String, String> bullets = new LinkedHashMap<>();
            Matcher b = BULLET.matcher(m.group(4));
            while (b.find()) {
                bullets.put(b.group(1), unindent(b.group(2)));
            }
            sections.put(number, new Section(number, Integer.parseInt(m.group(2)), m.group(3),
                    bullets.getOrDefault(CORRECT, ""),
                    bullets.getOrDefault(INCORRECT, ""),
                    bullets.getOrDefault(BONUS, "")));
        }
        return new ReviewDocument(key, summary, sections);
    }

    /** 새 리뷰로 해당 번호 섹션을 덮어쓰고 총평을 갱신한 문서. */
    public ReviewDocument merge(String newSummary, List<Section> updated) {
        Map<Integer, Section> merged = new TreeMap<>(sections);
        updated.forEach(sec -> merged.put(sec.number(), sec));
        return new ReviewDocument(key, nullToEmpty(newSummary).trim(), merged);
    }

    public boolean isReviewed(int number, String answerHash) {
        Section sec = sections.get(number);
        return sec != null && sec.answerHash().equals(answerHash);
    }

    public int totalScore() {
        return sections.values().stream().mapToInt(Section::score).sum();
    }

    public int maxScore() {
        return sections.size() * 10;
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(key.member()).append(" ").append(key.date()).append(" 리뷰  (")
          .append(totalScore()).append("/").append(maxScore()).append(")\n");
        if (!summary.isBlank()) sb.append("> 총평: ").append(summary.replace("\n", " ")).append("\n");
        sb.append("\n");
        for (Section sec : sections.values()) {
            sb.append(renderSection(sec)).append("\n");
        }
        return sb.toString();
    }

    /** 이슈 댓글에도 같은 형식을 쓴다. */
    public static String renderSection(Section sec) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Q").append(sec.number()).append("  —  ").append(sec.score()).append("/10\n");
        sb.append("<!-- answer-hash: ").append(sec.answerHash()).append(" -->\n");
        appendBullet(sb, CORRECT, sec.correct());
        appendBullet(sb, INCORRECT, sec.incorrect());
        appendBullet(sb, BONUS, sec.bonus());
        return sb.toString();
    }

    public static String hashAnswer(String answer) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(answer.strip().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest()).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void appendBullet(StringBuilder sb, String title, String body) {
        if (body == null || body.isBlank()) return;
        sb.append("- **").append(title).append("**: ").append(body.trim().replace("\n", "\n  ")).append("\n");
    }

    private static String unindent(String body) {
        return body.strip().replace("\n  ", "\n");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
