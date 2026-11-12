package com.licenseleak.invoices.api;

import com.licenseleak.invoices.application.InvoiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/invoice")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) { this.invoiceService = invoiceService; }

    @PostMapping
    public InvoiceResponse create(@PathVariable String caseId) {
        return invoiceService.create(caseId);
    }

    @GetMapping
    public InvoiceResponse get(@PathVariable String caseId) {
        return invoiceService.get(caseId);
    }
}
