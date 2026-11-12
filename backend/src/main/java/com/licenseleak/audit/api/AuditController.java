package com.licenseleak.audit.api;

import com.licenseleak.audit.application.AuditService;
import com.licenseleak.cases.application.CaseNotFoundException;
import com.licenseleak.cases.infrastructure.DemoCaseRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/audit")
public class AuditController {

    private final AuditService auditService;
    private final DemoCaseRepository caseRepository;

    public AuditController(AuditService auditService, DemoCaseRepository caseRepository) {
        this.auditService = auditService;
        this.caseRepository = caseRepository;
    }

    @GetMapping
    public AuditListResponse list(@PathVariable String caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new CaseNotFoundException(caseId);
        }
        return new AuditListResponse(auditService.listForCase(caseId));
    }

    public record AuditListResponse(List<AuditService.AuditItem> items) {
    }
}
