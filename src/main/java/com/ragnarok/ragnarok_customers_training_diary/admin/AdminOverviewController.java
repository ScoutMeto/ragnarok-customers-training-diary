package com.ragnarok.ragnarok_customers_training_diary.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin gym-wide overview — souhrn aktivit napříč všemi klienty.
 * Data se načítají v UI přes /api/analysis/admin/overview.
 */
@Controller
public class AdminOverviewController {

    @GetMapping("/admin/overview")
    public String overview() {
        return "admin/overview";
    }
}
