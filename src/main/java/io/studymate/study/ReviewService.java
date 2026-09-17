package io.studymate.study;

import io.studymate.github.GithubClient;
import io.studymate.llm.AnsweredQuestion;
import io.studymate.llm.ReviewResult;
import io.studymate.llm.StudyLlm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * 답변 커밋 → 리뷰 파일 커밋.
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

    public void reviewFor(String key) {
        String questionPath = paths.questionPath(key);
        GithubClient.RepoFile questionFile = github.readFile(questionPath)
                .orElseThrow(() -> new IllegalStateException("질문 파일을 찾을 수 없음: " + questionPath));

        QuestionDocument doc = QuestionDocument.parse(key, questionFile.content());
        List<QuestionDocument.Entry> answered = doc.answeredEntries();
        if (answered.isEmpty()) {
            log.info("답변 없음, 건너뜀 key={}", key);
            return;
        }

        String answersHash = hash(answered);
        String reviewPath = paths.reviewPath(key);
        var existingReview = github.readFile(reviewPath);
        if (existingReview.isPresent()
                && answersHash.equals(ReviewDocument.extractAnswersHash(existingReview.get().content()))) {
            log.info("동일 답변 이미 채점됨, 건너뜀 key={}", key);
            return;
        }

        String notePath = doc.sourcePath() != null ? doc.sourcePath() : paths.notePath(key);
        String note = github.readFile(notePath).map(GithubClient.RepoFile::content).orElse("");

        List<AnsweredQuestion> input = answered.stream()
                .map(e -> new AnsweredQuestion(e.number(), e.type(), e.question(), e.answer()))
                .toList();
        ReviewResult result = llm.review(note, input);

        String markdown = ReviewDocument.render(key, answersHash, result, answered.size());
        String existingSha = existingReview.map(GithubClient.RepoFile::sha).orElse(null);
        github.writeFile(reviewPath, markdown, "리뷰 생성 " + key, existingSha);
    }

    private static String hash(List<QuestionDocument.Entry> answered) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (QuestionDocument.Entry e : answered) {
                md.update((e.number() + ":" + e.answer().strip() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(md.digest()).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
