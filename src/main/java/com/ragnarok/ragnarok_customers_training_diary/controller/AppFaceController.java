package com.ragnarok.ragnarok_customers_training_diary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppFaceController {

    @GetMapping("/")
    public String redirectToIndex() {
        return "redirect:/index.html";
    }

}

