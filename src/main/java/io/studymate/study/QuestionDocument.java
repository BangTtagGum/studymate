package io.studymate.study;

import io.studymate.llm.GeneratedQuestion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * questions/YYYY-MM-DD.md 의 구조. 렌더링과 파싱을 한 곳에서 담당한다.
 *
 * <pre>
 * ## Q1. (concept) 질문 본문
 * &lt;!-- answer --&gt;
 * 학습자가 여기에 답을 씀
 * &lt;!-- /answer --&gt;
 * </pre>
 */
public record QuestionDocument(String date, String sourcePath, String sourceSha, List<Entry> entries) {

    public static final String ANSWER_OPEN = "<!-- answer -->";
    public static final String ANSWER_CLOSE = "<!-- /answer -->";

    private static final Pattern ENTRY = Pattern.compile(
            "^## Q(\\d+)\\.\\s*\\((\\w+)\\)\\s*(.+?)\\s*$\\n"
            + "\\s*" + Pattern.quote(ANSWER_OPEN) + "\\n(.*?)\\n?\\s*" + Pattern.quote(ANSWER_CLOSE),
            Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern SOURCE = Pattern.compile("^> 원본: (\\S+) @ (\\S+)$", Pattern.MULTILINE);

    public record Entry(int number, String type, String question, String answer) {
        public boolean answered() {
            return answer != null && !answer.isBlank();
        }
    }

    public static QuestionDocument fromGenerated(String date, String sourcePath, String sourceSha,
                                                 List<GeneratedQuestion> generated) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            GeneratedQuestion q = generated.get(i);
            entries.add(new Entry(i + 1, q.type(), q.question().trim(), ""));
        }
        return new QuestionDocument(date, sourcePath, sourceSha, entries);
    }

    public static QuestionDocument parse(String date, String markdown) {
        String sourcePath = null;
        String sourceSha = null;
        Matcher s = SOURCE.matcher(markdown);
        if (s.find()) {
            sourcePath = s.group(1);
            sourceSha = s.group(2);
        }
        List<Entry> entries = new ArrayList<>();
        Matcher m = ENTRY.matcher(markdown);
        while (m.find()) {
            entries.add(new Entry(
                    Integer.parseInt(m.group(1)),
                    m.group(2),
                    m.group(3).trim(),
                    m.group(4).strip()));
        }
        return new QuestionDocument(date, sourcePath, sourceSha, entries);
    }

    public List<Entry> answeredEntries() {
        return entries.stream().filter(Entry::answered).toList();
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(date).append(" 질문\n");
        sb.append("> 원본: ").append(sourcePath).append(" @ ").append(sourceSha).append("\n\n");
        sb.append("각 질문 아래 `").append(ANSWER_OPEN).append("` 와 `").append(ANSWER_CLOSE)
          .append("` 사이에 답을 적고 커밋하면 리뷰가 생성됩니다.\n\n");
        for (Entry e : entries) {
            sb.append("## Q").append(e.number()).append(". (").append(e.type()).append(") ")
              .append(e.question()).append("\n");
            sb.append(ANSWER_OPEN).append("\n");
            sb.append(e.answer() == null ? "" : e.answer()).append("\n");
            sb.append(ANSWER_CLOSE).append("\n\n");
        }
        return sb.toString();
    }
}
