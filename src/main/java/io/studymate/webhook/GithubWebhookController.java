package io.studymate.webhook;

import tools.jackson.databind.ObjectMapper;
import io.studymate.study.PushEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * GitHub 웹훅 진입점. 서명 검증 후 push 이벤트만 비동기 처리로 넘기고 즉시 응답한다
 * (GitHub 는 10초 안에 응답이 없으면 실패로 기록한다).
 */
@RestController
@RequestMapping("/webhook")
public class GithubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GithubWebhookController.class);

    private final WebhookSignatureVerifier verifier;
    private final ObjectMapper objectMapper;
    private final PushEventHandler pushEventHandler;

    public GithubWebhookController(WebhookSignatureVerifier verifier,
                                   ObjectMapper objectMapper,
                                   PushEventHandler pushEventHandler) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
        this.pushEventHandler = pushEventHandler;
    }

    @PostMapping("/github")
    public ResponseEntity<String> receive(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody byte[] body) throws IOException {

        if (!verifier.verify(body, signature)) {
            log.warn("서명 검증 실패 delivery={}", deliveryId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        if ("ping".equals(event)) {
            return ResponseEntity.ok("pong");
        }
        if (!"push".equals(event)) {
            return ResponseEntity.ok("ignored: " + event);
        }

        PushEvent pushEvent = objectMapper.readValue(body, PushEvent.class);
        pushEventHandler.handleAsync(pushEvent, deliveryId);
        return ResponseEntity.accepted().body("accepted");
    }
}
