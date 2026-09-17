package io.studymate.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.studymate.config.CommitMarker;
import io.studymate.config.StudyMateProperties;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GitHub REST API 클라이언트. Contents API 로 파일을 읽고 쓰고(쓰기는 곧 커밋),
 * Issues API 로 질문 이슈와 리뷰 댓글을 만든다. 로컬 clone 없이 동작하므로 서버에 git 이 필요 없다.
 */
@Component
public class GithubClient {

    private static final Logger log = LoggerFactory.getLogger(GithubClient.class);

    private final RestClient restClient;
    private final StudyMateProperties.Github github;

    public GithubClient(RestClient.Builder builder, StudyMateProperties properties) {
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

    public String repoFullName() {
        return github.owner() + "/" + github.repo();
    }

    /**
     * 이슈를 만든다. assignee 가 collaborator 가 아니면 GitHub 이 422 를 돌려주므로 assignee 없이 재시도한다.
     * 라벨은 없으면 GitHub 이 자동 생성한다.
     */
    public Issue createIssue(String title, String body, List<String> labels, List<String> assignees) {
        try {
            return postIssue(title, body, labels, assignees);
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            if (assignees.isEmpty()) throw e;
            log.warn("assignee 지정 실패, assignee 없이 재시도 assignees={}", assignees);
            return postIssue(title, body, labels, List.of());
        }
    }

    public void commentIssue(int number, String body) {
        restClient.post()
                .uri("/repos/{owner}/{repo}/issues/{number}/comments", github.owner(), github.repo(), number)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("body", body))
                .retrieve()
                .toBodilessEntity();
        log.info("이슈 댓글 작성 #{}", number);
    }

    public void closeIssue(int number) {
        restClient.patch()
                .uri("/repos/{owner}/{repo}/issues/{number}", github.owner(), github.repo(), number)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("state", "closed", "state_reason", "completed"))
                .retrieve()
                .toBodilessEntity();
        log.info("이슈 닫음 #{}", number);
    }

    private Issue postIssue(String title, String body, List<String> labels, List<String> assignees) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("title", title);
        req.put("body", body);
        req.put("labels", labels);
        if (!assignees.isEmpty()) req.put("assignees", assignees);

        Issue issue = restClient.post()
                .uri("/repos/{owner}/{repo}/issues", github.owner(), github.repo())
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(Issue.class);
        if (issue == null) throw new IllegalStateException("이슈 생성 응답이 비어 있음");
        log.info("이슈 생성 #{} {}", issue.number(), issue.htmlUrl());
        return issue;
    }

    public record RepoFile(String path, String content, String sha) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Issue(int number, @JsonProperty("html_url") String htmlUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentResponse(String content, String sha, String encoding) {
    }
}
