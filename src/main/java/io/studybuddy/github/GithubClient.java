package io.studybuddy.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.studybuddy.config.CommitMarker;
import io.studybuddy.config.StudyBuddyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * GitHub Contents API 로 파일을 읽고 쓴다. 쓰기는 곧 커밋이다.
 * 로컬 clone 없이 동작하므로 서버에 git 이 필요 없다.
 */
@Component
public class GithubClient {

    private static final Logger log = LoggerFactory.getLogger(GithubClient.class);

    private final RestClient restClient;
    private final StudyBuddyProperties.Github github;

    public GithubClient(RestClient.Builder builder, StudyBuddyProperties properties) {
        this.github = properties.github();
        this.restClient = builder
                .baseUrl(github.apiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + github.token())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /** 파일이 없으면 empty. */
    public Optional<RepoFile> readFile(String path) {
        try {
            ContentResponse res = restClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}",
                            github.owner(), github.repo(), path, github.branch())
                    .retrieve()
                    .body(ContentResponse.class);
            if (res == null || res.content() == null) return Optional.empty();
            String decoded = new String(
                    Base64.getMimeDecoder().decode(res.content()), StandardCharsets.UTF_8);
            return Optional.of(new RepoFile(path, decoded, res.sha()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    /**
     * 파일을 생성하거나 갱신한다. 커밋 메시지에는 봇 마커가 자동으로 붙는다.
     *
     * @param existingSha 갱신이면 기존 blob sha, 신규면 null
     */
    public void writeFile(String path, String content, String message, String existingSha) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", CommitMarker.PREFIX + " " + message);
        body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        body.put("branch", github.branch());
        body.put("committer", Map.of("name", github.botName(), "email", github.botEmail()));
        if (existingSha != null) body.put("sha", existingSha);

        restClient.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", github.owner(), github.repo(), path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        log.info("커밋 완료 path={} message={}", path, message);
    }

    public record RepoFile(String path, String content, String sha) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentResponse(String content, String sha, String encoding) {
    }
}
