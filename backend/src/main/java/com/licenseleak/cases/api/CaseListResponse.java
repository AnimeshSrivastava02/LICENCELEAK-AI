package com.licenseleak.cases.api;

import java.util.List;

public record CaseListResponse(List<CaseSummaryResponse> items) {
}
