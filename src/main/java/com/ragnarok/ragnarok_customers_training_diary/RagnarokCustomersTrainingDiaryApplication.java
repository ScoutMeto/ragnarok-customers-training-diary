package com.ragnarok.ragnarok_customers_training_diary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan("com.matejmarek.ragnarok_customers_training_diary.entity")
@SpringBootApplication
public class RagnarokCustomersTrainingDiaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagnarokCustomersTrainingDiaryApplication.class, args);
    }

}


/*
TODO - Řešíš trainingEntity a exerciseEntity(a jejich DTO)
-promysli, co za data chceš sbírat
-inspiruj se u minulého projektu, kde jsi už třídy vytvořil (chce to rozšířit a zpřesnit) -> složku máš otevřenou v PC (C:\Users\mpell\Desktop\IntelliJ IDEA projects\Ragnarok_TrainingDiaryAndReservationSystem - backend only\system_10-6-2023\ragnarokproject\src\main\java\com\mykettlebellproject\ragnarokproject\entity)
-logika zacházení s daty (ukládání, změny, načítání, delete)
-ověř propojení tabulek

-app.properties, pom.xml, AdminInitializer, WebConfiguration, DurationToLongConverter            ...hotovo (AdminSecurityConfiguration - nutno upravit s ohledem na aktuální projekt)
-složky: security, mapper  ...hotovo

    AKTUÁLNĚ ŘEŠÍM: ExerciseEntity a TrainingEntity (posléze mě čeká DTO)
 */
