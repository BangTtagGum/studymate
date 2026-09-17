package io.studymate.study;

import io.studymate.llm.GeneratedQuestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * questions/&lt;member&gt;/&lt;date&gt;.md 의 구조. 렌더링과 파싱을 한 곳에서 담당한다.
 * 이슈 댓글로 들어온 답변도 이 파일에 반영되므로, 파일이 곧 현재 상태다.
 *
 * <pre>
 * ## Q1. (concept) 질문 본문
 * &lt;!-- answer --&gt;
 * 학습자가 여기에 답을 씀
 * &lt;!-- /answer --&gt;
 * </pre>
 */
public record QuestionDocument(StudyKey key, String sourcePath, String sourceSha, Integer issueNumber,
                               List<Entry> entries) {

    public static final String ANSWER_OPEN = "<!-- answer -->";
    public static final String ANSWER_CLOSE = "<!-- /answer -->";

    private static final Pattern ENTRY = Pattern.compile(
            "^## Q(\\d+)\\.\\s*\\((\\w+)\\)\\s*(.+?)\\s*$\\n"
            + "\\s*" + Pattern.quote(ANSWER_OPEN) + "\\n(.*?)\\n?\\s*" + Pattern.quote(ANSWER_CLOSE),
            Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern SOURCE = Pattern.compile("^> 원본: (\\S+) @ (\\S+)$", Pattern.MULTILINE);
    private static final Pattern ISSUE = Pattern.compile("^> 이슈: #(\\d+)$", Pattern.MULTILINE);

    public record Entry(int number, String type, String question, String answer) {
        public boolean answered() {
            return answer != null && !answer.isBlank();
        }
    }

    public static QuestionDocument fromGenerated(StudyKey key, String sourcePath, String sourceSha,
                                                 List<GeneratedQuestion> generated) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            GeneratedQuestion q = generated.get(i);
            entries.add(new Entry(i + 1, q.type(), q.question().trim(), ""));
        }
        return new QuestionDocument(key, sourcePath, sourceSha, null, entries);
    }

    public static QuestionDocument parse(StudyKey key, String markdown) {
        String sourcePath = null;
        String sourceSha = null;
        Matcher s = SOURCE.matcher(markdown);
        if (s.find()) {
            sourcePath = s.group(1);
            sourceSha = s.group(2);
        }
        Integer issueNumber = null;
        Matcher i = ISSUE.matcher(markdown);
        if (i.find()) issueNumber = Integer.parseInt(i.group(1));

        List<Entry> entries = new ArrayList<>();
        Matcher m = ENTRY.matcher(markdown);
        while (m.find()) {
            entries.add(new Entry(
                    Integer.parseInt(m.group(1)),
                    m.group(2),
                    m.group(3).trim(),
                    m.group(4).strip()));
        }
        return new QuestionDocument(key, sourcePath, sourceSha, issueNumber, entries);
    }

    public QuestionDocument withIssueNumber(int number) {
        return new QuestionDocument(key, sourcePath, sourceSha, number, entries);
    }

    /** 번호별 답변을 덮어쓴 새 문서. 존재하지 않는 번호는 무시한다. */
    public QuestionDocument withAnswers(Map<Integer, String> answers) {
        List<Entry> merged = entries.stream()
                .map(e -> answers.containsKey(e.number())
                        ? new Entry(e.number(), e.type(), e.question(), answers.get(e.number()).strip())
                        : e)
                .toList();
        return new QuestionDocument(key, sourcePath, sourceSha, issueNumber, merged);
    }

    public List<Entry> answeredEntries() {
        return entries.stream().filter(Entry::answered).toList();
    }

    public boolean hasEntry(int number) {
        return entries.stream().anyMatch(e -> e.number() == number);
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(key.member()).append(" ").append(key.date()).append(" 질문\n");
        sb.append("> 원본: ").append(sourcePath).append(" @ ").append(sourceSha).append("\n");
        if (issueNumber != null) sb.append("> 이슈: #").append(issueNumber).append("\n");
        sb.append("\n");
        sb.append("이슈에 `Q1. 답변` 형식으로 댓글을 달거나, 아래 `").append(ANSWER_OPEN).append("` 와 `")
          .append(ANSWER_CLOSE).append("` 사이에 답을 적고 커밋하면 리뷰가 생성됩니다.\n\n");
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
