package com.berdachuk.expertmatch.core.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Web controller for Visual Graph admin page (graph visualization).
 */
@Slf4j
@Controller
@RequestMapping("/admin/visual-graph")
public class VisualGraphWebController {

    @GetMapping
    public String visualGraphPage(Model model) {
        model.addAttribute("currentPage", "visual-graph");
        return "admin/visual-graph";
    }
}
