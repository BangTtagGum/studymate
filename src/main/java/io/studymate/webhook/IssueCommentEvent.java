package io.studymate.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * GitHub issue_comment 이벤트 페이로드 중 필요한 부분만 매핑.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueCommentEvent(
        String action,
        Issue issue,
        Comment comment,
        Repository repository
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Issue(int number, String body, String state, List<Label> labels) {
        public Issue {
            labels = labels == null ? List.of() : labels;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Comment(long id, String body, User user) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(String login) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Label(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(@JsonProperty("full_name") String fullName) {
    }

    public boolean isCreatedOrEdited() {
        return "created".equals(action) || "edited".equals(action);
    }
}
