package com.ragnarok.ragnarok_customers_training_diary.lesson;

import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Klientský pohled na plán lekcí. Read-only, omezený na rozsah ±7 dní od dneška —
 * trenér zveřejňuje, klient vidí, do minulosti se klient nedostane.
 */
@Controller
@RequestMapping("/lessons")
public class ClientLessonController {

    private final GroupLessonPlanService lessonService;

    public ClientLessonController(GroupLessonPlanService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public String list(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("lessons", lessonService.listForClient(today));
        model.addAttribute("today", today);
        return "lessons/list";
    }
}
