package com.ragnarok.ragnarok_customers_training_diary.analysis;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogService;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalysisPageController {

    private final ExerciseCatalogService catalogService;
    private final TrainingService trainingService;

    public AnalysisPageController(ExerciseCatalogService catalogService,
                                  TrainingService trainingService) {
        this.catalogService = catalogService;
        this.trainingService = trainingService;
    }

    @GetMapping("/analysis")
    public String analysisPage(@AuthenticationPrincipal AccountEntity user, Model model) {
        model.addAttribute("catalogItems", catalogService.listVisibleTo(user));
        // Phase 14 (A16): seznam cviků označených korunkou — sekce na konci stránky
        model.addAttribute("starredExercises", trainingService.listStarredExercises(user));
        return "analysis/index";
    }
}
