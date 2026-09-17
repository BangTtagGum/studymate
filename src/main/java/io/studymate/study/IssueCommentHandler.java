package io.studymate.study;

import io.studymate.github.GithubClient;
import io.studymate.webhook.IssueCommentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * issue_comment 이벤트를 받아 질문 이슈에 달린 답변 댓글만 골라 채점으로 넘긴다.
 */
@Component
public class IssueCommentHandler {

    private static final Logger log = LoggerFactory.getLogger(IssueCommentHandler.class);

    private final ReviewService reviewService;
    private final String repoFullName;

    public IssueCommentHandler(ReviewService reviewService, GithubClient github) {
        this.reviewService = reviewService;
        this.repoFullName = github.repoFullName();
    }

    @Async
    public void handleAsync(IssueCommentEvent event, String deliveryId) {
        try {
            handle(event);
        } catch (Exception e) {
            log.error("issue_comment 처리 실패 delivery={}", deliveryId, e);
        }
    }

    public void handle(IssueCommentEvent event) {
        if (!event.isCreatedOrEdited() || event.issue() == null || event.comment() == null) {
            log.info("처리 대상 아님 action={}", event.action());
            return;
        }
        if (event.repository() != null && !repoFullName.equalsIgnoreCase(event.repository().fullName())) {
            log.info("대상 저장소 아님, 무시 repo={}", event.repository().fullName());
            return;
        }
        if (IssueMarkdown.isBotComment(event.comment().body())) {
            log.info("봇 자신의 댓글, 무시 issue=#{}", event.issue().number());
            return;
        }
        Optional<StudyKey> key = IssueMarkdown.keyFrom(event.issue().body());
        if (key.isEmpty()) {
            log.info("질문 이슈 아님, 무시 issue=#{}", event.issue().number());
            return;
        }
        Map<Integer, String> answers = CommentAnswers.parse(event.comment().body());
        if (answers.isEmpty()) {
            log.info("답변 형식 아님, 무시 issue=#{} comment={}", event.issue().number(), event.comment().id());
            return;
        }
        log.info("답변 수신 key={} issue=#{} questions={}", key.get(), event.issue().number(), answers.keySet());
        reviewService.answerFromIssue(key.get(), answers);
    }
}
