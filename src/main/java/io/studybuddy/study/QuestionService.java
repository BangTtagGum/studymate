package io.studybuddy.study;

import io.studybuddy.config.StudyBuddyProperties;
import io.studybuddy.github.GithubClient;
import io.studybuddy.llm.GeneratedQuestions;
import io.studybuddy.llm.StudyLlm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 노트 커밋 → 질문 파일 커밋.
 */
@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final GithubClient github;
    private final StudyLlm llm;
    private final StudyPaths paths;
    private final int questionCount;

    public QuestionService(GithubClient github, StudyLlm llm, StudyPaths paths, StudyBuddyProperties properties) {
        this.github = github;
        this.llm = llm;
        this.paths = paths;
        this.questionCount = properties.study().questionCount();
    }

    public void generateFor(String key) {
        String questionPath = paths.questionPath(key);
        if (github.readFile(questionPath).isPresent()) {
            log.info("이미 질문 파일 존재, 건너뜀 key={}", key);
            return;
        }

        String notePath = paths.notePath(key);
        GithubClient.RepoFile note = github.readFile(notePath)
                .orElseThrow(() -> new IllegalStateException("노트 파일을 찾을 수 없음: " + notePath));
        if (note.content().isBlank()) {
            log.info("노트가 비어 있음, 건너뜀 key={}", key);
            return;
        }

        GeneratedQuestions generated = llm.generateQuestions(note.content(), questionCount);
        if (generated.questions().isEmpty()) {
            log.warn("생성된 질문 없음 key={}", key);
            return;
        }

        QuestionDocument doc = QuestionDocument.fromGenerated(key, notePath, note.sha(), generated.questions());
        github.writeFile(questionPath, doc.render(), "질문 생성 " + key, null);
    }
}
