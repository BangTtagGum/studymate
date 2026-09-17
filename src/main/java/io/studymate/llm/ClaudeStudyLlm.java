package io.studymate.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Anthropic Java SDK 로 Claude 를 호출한다. 구조화 출력(output_config)으로 JSON 을 바로 record 에 매핑한다.
 */
public class ClaudeStudyLlm implements StudyLlm {

    private static final Logger log = LoggerFactory.getLogger(ClaudeStudyLlm.class);
    private static final long MAX_TOKENS = 16_000L;

    private final AnthropicClient client;
    private final String model;
    private final String questionSystemPrompt = PromptLoader.load("question.md");
    private final String reviewSystemPrompt = PromptLoader.load("review.md");

    public ClaudeStudyLlm(AnthropicClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public GeneratedQuestions generateQuestions(String noteMarkdown, int count) {
        String user = """
                질문 개수: %d

                <note>
                %s
                </note>
                """.formatted(count, noteMarkdown);

        StructuredMessageCreateParams<GeneratedQuestions> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(questionSystemPrompt)
                .outputConfig(GeneratedQuestions.class)
                .addUserMessage(user)
                .build();

        GeneratedQuestions result = firstStructured(client.messages().create(params).content());
        log.info("질문 {}개 생성", result.questions().size());
        return result;
    }

    @Override
    public ReviewResult review(String noteMarkdown, List<AnsweredQuestion> answered) {
        StringBuilder qa = new StringBuilder();
        for (AnsweredQuestion a : answered) {
            qa.append("### Q").append(a.number()).append(" (").append(a.type()).append(")\n")
              .append(a.question()).append("\n\n")
              .append("학습자 답변:\n").append(a.answer()).append("\n\n");
        }
        String user = """
                <note>
                %s
                </note>

                <answers>
                %s
                </answers>
                """.formatted(noteMarkdown, qa);

        StructuredMessageCreateParams<ReviewResult> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(reviewSystemPrompt)
                .outputConfig(ReviewResult.class)
                .addUserMessage(user)
                .build();

        ReviewResult result = firstStructured(client.messages().create(params).content());
        log.info("리뷰 {}건 생성", result.reviews().size());
        return result;
    }

    private static <T> T firstStructured(List<com.anthropic.models.messages.StructuredContentBlock<T>> content) {
        return content.stream()
                .flatMap(cb -> cb.text().stream())
                .map(t -> t.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude 응답에 텍스트 블록이 없음"));
    }
}
