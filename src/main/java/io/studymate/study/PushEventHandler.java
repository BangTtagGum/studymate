package io.studymate.study;

import io.studymate.config.StudyMateProperties;
import io.studymate.webhook.PushEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * push 이벤트를 받아 변경 파일 경로로 분기한다.
 * notes/&lt;member&gt;/*.md → 질문 생성, questions/&lt;member&gt;/*.md → 채점. 그 외는 무시.
 */
@Component
public class PushEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PushEventHandler.class);

    private final QuestionService questionService;
    private final ReviewService reviewService;
    private final StudyPaths paths;
    private final String branch;
    private final String botName;

    public PushEventHandler(QuestionService questionService, ReviewService reviewService,
                            StudyPaths paths, StudyMateProperties properties) {
        this.questionService = questionService;
        this.reviewService = reviewService;
        this.paths = paths;
        this.branch = properties.github().branch();
        this.botName = properties.github().botName();
    }

    @Async
    public void handleAsync(PushEvent event, String deliveryId) {
        try {
            handle(event);
        } catch (Exception e) {
            log.error("push 처리 실패 delivery={}", deliveryId, e);
        }
    }

    public void handle(PushEvent event) {
        if (!branch.equals(event.branch())) {
            log.info("대상 브랜치 아님, 무시 ref={}", event.ref());
            return;
        }
        if (event.pusher() != null && botName.equals(event.pusher().name())) {
            log.info("봇 자신의 push, 무시");
            return;
        }

        Set<StudyKey> noteKeys = new LinkedHashSet<>();
        Set<StudyKey> questionKeys = new LinkedHashSet<>();
        for (String path : event.changedPathsFromHumans()) {
            paths.noteKey(path).ifPresent(noteKeys::add);
            paths.questionKey(path).ifPresent(questionKeys::add);
        }
        if (noteKeys.isEmpty() && questionKeys.isEmpty()) {
            log.info("처리 대상 파일 없음");
            return;
        }

        noteKeys.forEach(questionService::generateFor);
        questionKeys.forEach(reviewService::reviewFor);
    }
}
