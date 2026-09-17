package io.studybuddy;

import io.studybuddy.config.StudyBuddyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableConfigurationProperties(StudyBuddyProperties.class)
@SpringBootApplication
public class StudyBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyBuddyApplication.class, args);
    }
}
