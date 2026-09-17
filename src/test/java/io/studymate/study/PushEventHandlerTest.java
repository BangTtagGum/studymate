package io.studymate.study;

import io.studymate.config.StudyMateProperties;
import io.studymate.webhook.PushEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PushEventHandlerTest {

    private QuestionService questionService;
    private ReviewService reviewService;
    private PushEventHandler handler;

    @BeforeEach
    void setUp() {
        StudyMateProperties props = new StudyMateProperties(
                new StudyMateProperties.Github("o", "r", "main", "t", null, null, null, null),
                new StudyMateProperties.Claude(null, null),
                new StudyMateProperties.Study(null, null, null, 0));
        questionService = mock(QuestionService.class);
        reviewService = mock(ReviewService.class);
        handler = new PushEventHandler(questionService, reviewService, new StudyPaths(props), props);
    }

    @Test
    void routes_notes_to_question_and_questions_to_review() {
        PushEvent event = push("refs/heads/main", "jylee",
                commit("study: db", List.of("notes/2026-09-17.md"), List.of("questions/2026-09-16.md")));

        handler.handle(event);

        verify(questionService).generateFor("2026-09-17");
        verify(reviewService).reviewFor("2026-09-16");
    }

    @Test
    void ignores_bot_commits_and_other_branches() {
        handler.handle(push("refs/heads/main", "jylee",
                commit("[studymate] 질문 생성", List.of("questions/2026-09-17.md"), List.of())));
        handler.handle(push("refs/heads/feature", "jylee",
                commit("study", List.of("notes/2026-09-17.md"), List.of())));
        handler.handle(push("refs/heads/main", "studymate",
                commit("study", List.of("notes/2026-09-17.md"), List.of())));

        verifyNoInteractions(questionService, reviewService);
    }

    @Test
    void ignores_unrelated_paths() {
        handler.handle(push("refs/heads/main", "jylee",
                commit("chore", List.of("README.md", "reviews/2026-09-17.md", "notes/sub/x.md"), List.of())));

        verifyNoInteractions(questionService, reviewService);
    }

    private static PushEvent push(String ref, String pusher, PushEvent.Commit... commits) {
        return new PushEvent(ref, "0", "1", List.of(commits), new PushEvent.Pusher(pusher, null));
    }

    private static PushEvent.Commit commit(String message, List<String> added, List<String> modified) {
        return new PushEvent.Commit("sha", message, added, modified, List.of(), null);
    }
}
