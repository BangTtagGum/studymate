package io.studymate.study;

import io.studymate.github.GithubClient;
import io.studymate.llm.AnsweredQuestion;
import io.studymate.llm.QuestionReview;
import io.studymate.llm.ReviewResult;
import io.studymate.llm.StudyLlm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 답변 → 채점. 답변은 이슈 댓글 또는 질문 파일 직접 편집 두 경로로 들어오며,
 * 어느 쪽이든 질문 파일에 반영된 뒤 이 서비스가 "아직 채점 안 된 답변"만 골라 리뷰한다.
 */
@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final GithubClient github;
    private final StudyLlm llm;
    private final StudyPaths paths;

    public ReviewService(GithubClient github, StudyLlm llm, StudyPaths paths) {
        this.github = github;
        this.llm = llm;
        this.paths = paths;
    }

    /** 이슈 댓글로 들어온 답변을 질문 파일에 반영한 뒤 채점한다. */
    public void answerFromIssue(StudyKey key, Map<Integer, String> answers) {
        String questionPath = paths.questionPath(key);
        GithubClient.RepoFile file = github.readFile(questionPath)
                .orElseThrow(() -> new IllegalStateException("질문 파일을 찾을 수 없음: " + questionPath));
        QuestionDocument doc = QuestionDocument.parse(key, file.content());

        Map<Integer, String> known = answers.entrySet().stream()
                .filter(e -> doc.hasEntry(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (known.isEmpty()) {
            log.info("댓글의 질문 번호가 문서와 맞지 않음 key={} numbers={}", key, answers.keySet());
            return;
        }

        QuestionDocument updated = doc.withAnswers(known);
        if (!updated.equals(doc)) {
            github.writeFile(questionPath, updated.render(), "답변 반영 " + key + " " + known.keySet(), file.sha());
        }
        reviewFor(key);
    }

    /** 질문 파일의 답변 중 아직 채점되지 않은 것을 리뷰하고, 파일과 이슈에 기록한다. */
    public void reviewFor(StudyKey key) {
        String questionPath = paths.questionPath(key);
        GithubClient.RepoFile questionFile = github.readFile(questionPath)
                .orElseThrow(() -> new IllegalStateException("질문 파일을 찾을 수 없음: " + questionPath));
        QuestionDocument doc = QuestionDocument.parse(key, questionFile.content());

        String reviewPath = paths.reviewPath(key);
        var existingReview = github.readFile(reviewPath);
        ReviewDocument review = existingReview
                .map(f -> ReviewDocument.parse(key, f.content()))
                .orElse(ReviewDocument.empty(key));

        List<QuestionDocument.Entry> pending = doc.answeredEntries().stream()
                .filter(e -> !review.isReviewed(e.number(), ReviewDocument.hashAnswer(e.answer())))
                .toList();
        if (pending.isEmpty()) {
            log.info("채점할 새 답변 없음 key={}", key);
            return;
        }

        String note = github.readFile(doc.sourcePath() != null ? doc.sourcePath() : paths.notePath(key))
                .map(GithubClient.RepoFile::content).orElse("");
        List<AnsweredQuestion> input = pending.stream()
                .map(e -> new AnsweredQuestion(e.number(), e.type(), e.question(), e.answer()))
                .toList();
        ReviewResult result = llm.review(note, input);

        List<ReviewDocument.Section> sections = new ArrayList<>();
        for (QuestionReview r : result.reviews()) {
            pending.stream().filter(e -> e.number() == r.number()).findFirst()
                    .ifPresent(e -> sections.add(ReviewDocument.Section.of(r, ReviewDocument.hashAnswer(e.answer()))));
        }
        ReviewDocument merged = review.merge(result.summary(), sections);

        String existingSha = existingReview.map(GithubClient.RepoFile::sha).orElse(null);
        github.writeFile(reviewPath, merged.render(), "리뷰 " + key + " Q" + numbers(sections), existingSha);

        if (doc.issueNumber() == null) return;
        github.commentIssue(doc.issueNumber(), IssueMarkdown.reviewComment(result.summary(), sections));

        boolean allReviewed = doc.entries().stream().allMatch(e -> merged.sections().containsKey(e.number()));
        if (allReviewed) {
            github.commentIssue(doc.issueNumber(), IssueMarkdown.completedComment(merged, reviewPath));
            github.closeIssue(doc.issueNumber());
        }
    }

    private static String numbers(List<ReviewDocument.Section> sections) {
        return String.join(",", sections.stream().map(s -> String.valueOf(s.number())).toList());
    }
}
