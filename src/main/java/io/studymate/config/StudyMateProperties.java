package io.studymate.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml 의 studymate.* 설정.
 */
@Validated
@ConfigurationProperties(prefix = "studymate")
public record StudyMateProperties(
        Github github,
        Claude claude,
        Study study
) {

    public record Github(
            @NotBlank String owner,
            @NotBlank String repo,
            @NotBlank String branch,
            @NotBlank String token,
            String webhookSecret,
            String botName,
            String botEmail,
            String apiBaseUrl
    ) {
        public Github {
            if (botName == null || botName.isBlank()) botName = "studymate";
            if (botEmail == null || botEmail.isBlank()) botEmail = "studymate@users.noreply.github.com";
            if (apiBaseUrl == null || apiBaseUrl.isBlank()) apiBaseUrl = "https://api.github.com";
        }
    }

    public record Claude(
            String apiKey,
            String model
    ) {
        public Claude {
            if (model == null || model.isBlank()) model = "claude-opus-5";
        }

        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Study(
            String notesDir,
            String questionsDir,
            String reviewsDir,
            int questionCount
    ) {
        public Study {
            if (notesDir == null || notesDir.isBlank()) notesDir = "notes";
            if (questionsDir == null || questionsDir.isBlank()) questionsDir = "questions";
            if (reviewsDir == null || reviewsDir.isBlank()) reviewsDir = "reviews";
            if (questionCount <= 0) questionCount = 4;
        }
    }
}
