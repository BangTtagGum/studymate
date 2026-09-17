package io.studymate.study;

import io.studymate.github.GithubClient;
import io.studymate.webhook.IssueCommentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IssueCommentHandlerTest {

    private static final StudyKey KEY = new StudyKey("jylee", "2026-09-17");
    private static final String ISSUE_BODY = "<!-- studymate:questions key=jylee/2026-09-17 -->\n@jylee 님";

    private ReviewService reviewService;
    private IssueCommentHandler handler;

    @BeforeEach
    void setUp() {
        reviewService = mock(ReviewService.class);
        GithubClient github = mock(GithubClient.class);
        when(github.repoFullName()).thenReturn("o/r");
        handler = new IssueCommentHandler(reviewService, github);
    }

    @Test
    void answer_comment_on_question_issue_is_reviewed() {
        handler.handle(event("created", ISSUE_BODY, "Q1. 답변입니다\nQ2. 둘째", "o/r"));

        verify(reviewService).answerFromIssue(KEY, Map.of(1, "답변입니다", 2, "둘째"));
    }

    @Test
    void edited_comment_is_reviewed_again() {
        handler.handle(event("edited", ISSUE_BODY, "Q1. 수정한 답변", "o/r"));

        verify(reviewService).answerFromIssue(KEY, Map.of(1, "수정한 답변"));
    }

    @Test
    void ignores_bot_comments_other_issues_other_repos_and_deletions() {
        handler.handle(event("created", ISSUE_BODY, "<!-- studymate:review -->\n## Q1 — 8/10", "o/r"));
        handler.handle(event("created", "일반 이슈", "Q1. 답변", "o/r"));
        handler.handle(event("created", ISSUE_BODY, "Q1. 답변", "someone/else"));
        handler.handle(event("deleted", ISSUE_BODY, "Q1. 답변", "o/r"));
        handler.handle(event("created", ISSUE_BODY, "질문 있어요, 이거 왜 이래요?", "o/r"));

        verifyNoInteractions(reviewService);
    }

    private static IssueCommentEvent event(String action, String issueBody, String commentBody, String repo) {
        return new IssueCommentEvent(action,
                new IssueCommentEvent.Issue(7, issueBody, "open", List.of(new IssueCommentEvent.Label("studymate"))),
                new IssueCommentEvent.Comment(1L, commentBody, new IssueCommentEvent.User("jylee")),
                new IssueCommentEvent.Repository(repo));
    }
}
