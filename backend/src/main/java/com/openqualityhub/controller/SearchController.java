package com.openqualityhub.controller;

import com.openqualityhub.dto.response.SearchResultsResponse;
import com.openqualityhub.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Global full-text search across entities")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search across all entity types")
    public ResponseEntity<SearchResultsResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(searchService.search(q, type, limit));
    }
}
