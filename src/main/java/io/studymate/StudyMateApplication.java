package io.studymate;

import io.studymate.config.StudyMateProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableConfigurationProperties(StudyMateProperties.class)
@SpringBootApplication
public class StudyMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyMateApplication.class, args);
    }
}
