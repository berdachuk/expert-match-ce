package com.berdachuk.expertmatch.core.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Web controller for the Test data admin page (generate full dataset with embeddings and graph).
 */
@Slf4j
@Controller
@RequestMapping("/admin/test-data")
public class TestDataWebController {

    @GetMapping
    public String testDataPage(Model model) {
        model.addAttribute("currentPage", "test-data");
        return "admin/test-data";
    }
}
