package com.berdachuk.expertmatch.core.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Web controller for the Ingest admin page (ingest from external database, embeddings, graph).
 */
@Slf4j
@Controller
@RequestMapping("/admin/ingest")
public class IngestWebController {

    @GetMapping
    public String ingestPage(Model model) {
        model.addAttribute("currentPage", "ingest");
        return "admin/ingest";
    }
}
