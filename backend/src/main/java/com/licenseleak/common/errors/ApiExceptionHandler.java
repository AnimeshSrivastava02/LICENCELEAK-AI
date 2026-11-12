package com.licenseleak.common.errors;

import com.licenseleak.cases.application.CaseNotFoundException;
import com.licenseleak.analysis.application.AnalysisNotFoundException;
import com.licenseleak.analysis.application.AnalysisProviderException;
import com.licenseleak.common.errors.ConflictException;
import com.licenseleak.invoices.application.InvoiceNotFoundException;
import com.licenseleak.invoices.application.InvoiceProviderException;
import com.licenseleak.invoices.application.InvalidWebhookException;
import com.licenseleak.approvals.application.DecisionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CaseNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCaseNotFound(CaseNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("CASE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAnalysisNotFound(AnalysisNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("ANALYSIS_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(AnalysisProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleAnalysisProviderUnavailable(AnalysisProviderException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("ANALYSIS_UNAVAILABLE", exception.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleInvoiceNotFound(InvoiceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("INVOICE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvoiceProviderException.class)
    public ResponseEntity<ApiErrorResponse> handleInvoiceProviderFailure(InvoiceProviderException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse("INVOICE_PROVIDER_FAILURE", exception.getMessage()));
    }

    @ExceptionHandler(InvalidWebhookException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidWebhook(InvalidWebhookException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_WEBHOOK", exception.getMessage()));
    }

    @ExceptionHandler(DecisionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDecisionNotFound(DecisionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("DECISION_NOT_FOUND", exception.getMessage()));
    }
}
