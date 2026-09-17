package io.studymate.webhook;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 호스팅 플랫폼 헬스체크용.
 */
@RestController
public class HealthController {

    @GetMapping("/healthz")
    public String health() {
        return "ok";
    }
}
