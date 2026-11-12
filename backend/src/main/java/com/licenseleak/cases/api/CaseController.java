package com.licenseleak.cases.api;

import com.licenseleak.cases.application.CaseQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

    private final CaseQueryService caseQueryService;

    public CaseController(CaseQueryService caseQueryService) {
        this.caseQueryService = caseQueryService;
    }

    @GetMapping
    public CaseListResponse listCases() {
        return new CaseListResponse(caseQueryService.listCases());
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseDetailResponse> getCase(@PathVariable String caseId) {
        return ResponseEntity.ok(caseQueryService.getCase(caseId));
    }
}
