package com.ragnarok.ragnarok_customers_training_diary.configuration;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * ScoutMeto kolo 8 (review fix): vygenerovaná tabulka série (Ladder/Stepladder/Pyramid)
 * může mít víc než 256 řádků (peak 23+ s krokem 1 → 276 řádků), což by narazilo na
 * default Spring autoGrowCollectionLimit=256 a shodilo binding celého formuláře (500).
 */
@ControllerAdvice
public class FormBindingConfiguration {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(2048);
    }
}
