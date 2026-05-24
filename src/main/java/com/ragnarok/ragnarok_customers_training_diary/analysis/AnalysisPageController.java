package com.ragnarok.ragnarok_customers_training_diary.analysis;

import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalysisPageController {

    private final ExerciseCatalogService catalogService;

    public AnalysisPageController(ExerciseCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/analysis")
    public String analysisPage(Model model) {
        model.addAttribute("catalogItems", catalogService.listAll());
        return "analysis/index";
    }
}
