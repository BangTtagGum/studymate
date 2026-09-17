package io.studymate.config;

/**
 * 봇이 만든 커밋을 식별하는 접두어. 웹훅 루프 방지에 사용한다.
 */
public final class CommitMarker {

    public static final String PREFIX = "[studymate]";

    private CommitMarker() {
    }

    public static boolean isBotCommit(String message) {
        return message != null && message.trim().startsWith(PREFIX);
    }
}
