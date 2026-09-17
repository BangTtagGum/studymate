package io.studybuddy.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API 키가 없을 때 파이프라인만 검증하기 위한 더미 구현.
 * 노트의 헤딩을 뽑아 질문처럼 만들고, 리뷰는 고정 문구를 돌려준다.
 */
public class DummyStudyLlm implements StudyLlm {

    private static final Pattern HEADING = Pattern.compile("^#{1,4}\\s+(.+)$", Pattern.MULTILINE);

    @Override
    public GeneratedQuestions generateQuestions(String noteMarkdown, int count) {
        List<GeneratedQuestion> questions = new ArrayList<>();
        Matcher m = HEADING.matcher(noteMarkdown);
        while (m.find() && questions.size() < count) {
            String topic = m.group(1).trim();
            questions.add(new GeneratedQuestion("concept",
                    "'" + topic + "' 에 대해 노트를 보지 않고 자신의 말로 설명해보세요."));
        }
        if (questions.size() < count) {
            questions.add(new GeneratedQuestion("apply",
                    "오늘 정리한 내용을 실제 프로젝트에 적용한다면 어떤 상황에서 쓸 수 있을까요?"));
        }
        return new GeneratedQuestions(questions);
    }

    @Override
    public ReviewResult review(String noteMarkdown, List<AnsweredQuestion> answered) {
        List<QuestionReview> reviews = answered.stream()
                .map(a -> new QuestionReview(a.number(), 0,
                        "(더미) 답변 길이: " + a.answer().length() + "자",
                        "",
                        "ANTHROPIC_API_KEY 를 설정하면 실제 채점이 이루어집니다."))
                .toList();
        return new ReviewResult("더미 리뷰 — API 키 미설정 상태로 파이프라인만 검증했습니다.", reviews);
    }
}
