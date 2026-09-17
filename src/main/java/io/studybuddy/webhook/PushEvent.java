package io.studybuddy.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * GitHub push 이벤트 페이로드 중 필요한 부분만 매핑.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushEvent(
        String ref,
        String before,
        String after,
        List<Commit> commits,
        Pusher pusher
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Commit(
            String id,
            String message,
            List<String> added,
            List<String> modified,
            List<String> removed,
            Author author
    ) {
        public Commit {
            added = added == null ? List.of() : added;
            modified = modified == null ? List.of() : modified;
            removed = removed == null ? List.of() : removed;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(String name, String email, String username) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pusher(String name, String email) {
    }

    public PushEvent {
        commits = commits == null ? List.of() : commits;
    }

    /** refs/heads/main → main */
    public String branch() {
        return ref == null ? "" : ref.replaceFirst("^refs/heads/", "");
    }

    /** 사람이 만든 커밋에서 추가·수정된 파일 경로 (봇 커밋 제외). */
    public Set<String> changedPathsFromHumans() {
        Set<String> paths = new TreeSet<>();
        for (Commit commit : commits) {
            if (io.studybuddy.config.CommitMarker.isBotCommit(commit.message())) continue;
            paths.addAll(commit.added());
            paths.addAll(commit.modified());
        }
        return paths;
    }
}
