package com.licenseleak.approvals.api;

import com.licenseleak.approvals.application.DecisionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/decision")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) { this.decisionService = decisionService; }

    @PostMapping
    public DecisionResponse decide(@PathVariable String caseId, @Valid @RequestBody DecisionRequest request) {
        return decisionService.decide(caseId, request);
    }

    @GetMapping
    public DecisionResponse latest(@PathVariable String caseId) {
        return decisionService.latest(caseId);
    }
}
