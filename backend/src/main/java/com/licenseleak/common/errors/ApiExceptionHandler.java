package com.licenseleak.common.errors;

import com.licenseleak.cases.application.CaseNotFoundException;
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
}
