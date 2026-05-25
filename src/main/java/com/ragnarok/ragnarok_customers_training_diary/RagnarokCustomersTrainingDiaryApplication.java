package com.ragnarok.ragnarok_customers_training_diary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RagnarokCustomersTrainingDiaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagnarokCustomersTrainingDiaryApplication.class, args);
    }
}
