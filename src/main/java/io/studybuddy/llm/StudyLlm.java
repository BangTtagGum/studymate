package io.studybuddy.llm;

import java.util.List;

/**
 * 질문 생성·채점 LLM 추상화. 실제 구현은 Claude, API 키가 없으면 Dummy 가 쓰인다.
 */
public interface StudyLlm {

    GeneratedQuestions generateQuestions(String noteMarkdown, int count);

    ReviewResult review(String noteMarkdown, List<AnsweredQuestion> answered);
}
