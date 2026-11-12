package com.licenseleak.analysis.api;

import com.licenseleak.analysis.application.AnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public AnalysisResponse analyze(@PathVariable String caseId) {
        return analysisService.analyze(caseId);
    }

    @GetMapping
    public AnalysisResponse latest(@PathVariable String caseId) {
        return analysisService.latest(caseId);
    }
}
