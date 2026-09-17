package io.studymate.webhook;

import io.studymate.study.IssueCommentHandler;
import io.studymate.study.PushEventHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GithubWebhookController.class)
class GithubWebhookControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    WebhookSignatureVerifier verifier;

    @MockitoBean
    PushEventHandler pushEventHandler;

    @MockitoBean
    IssueCommentHandler issueCommentHandler;

    @Test
    void push_event_is_accepted_and_dispatched() throws Exception {
        when(verifier.verify(any(), any())).thenReturn(true);

        mvc.perform(post("/webhook/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", "d1")
                        .contentType("application/json")
                        .content("{\"ref\":\"refs/heads/main\",\"commits\":[]}"))
                .andExpect(status().isAccepted());

        verify(pushEventHandler).handleAsync(any(PushEvent.class), eq("d1"));
    }

    @Test
    void issue_comment_event_is_accepted_and_dispatched() throws Exception {
        when(verifier.verify(any(), any())).thenReturn(true);

        mvc.perform(post("/webhook/github")
                        .header("X-GitHub-Event", "issue_comment")
                        .header("X-GitHub-Delivery", "d2")
                        .contentType("application/json")
                        .content("{\"action\":\"created\",\"issue\":{\"number\":7,\"body\":\"\"},"
                                + "\"comment\":{\"id\":1,\"body\":\"Q1. a\",\"user\":{\"login\":\"jylee\"}},"
                                + "\"repository\":{\"full_name\":\"o/r\"}}"))
                .andExpect(status().isAccepted());

        verify(issueCommentHandler).handleAsync(any(IssueCommentEvent.class), eq("d2"));
    }

    @Test
    void unrelated_events_are_ignored() throws Exception {
        when(verifier.verify(any(), any())).thenReturn(true);

        mvc.perform(post("/webhook/github")
                        .header("X-GitHub-Event", "issues")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        verifyNoInteractions(pushEventHandler, issueCommentHandler);
    }

    @Test
    void invalid_signature_is_rejected() throws Exception {
        when(verifier.verify(any(), any())).thenReturn(false);

        mvc.perform(post("/webhook/github")
                        .header("X-GitHub-Event", "push")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ping_returns_pong() throws Exception {
        when(verifier.verify(any(), any())).thenReturn(true);

        mvc.perform(post("/webhook/github")
                        .header("X-GitHub-Event", "ping")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
