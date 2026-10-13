package com.company.recsys.controller;

import com.company.recsys.dto.RecommendationDto;
import com.company.recsys.dto.SearchResultDto;
import com.company.recsys.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for recommendation endpoints.
 * Provides semantic search and item-to-item recommendations.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations", description = "Semantic search and article recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Semantic search by query text.
     * GET /api/v1/recommendations/search?q={query}&limit=5
     */
    @GetMapping("/search")
    @Operation(summary = "Semantic search", description = "Search articles by natural language query using vector similarity")
    public ResponseEntity<List<SearchResultDto>> search(
            @Parameter(description = "Search query text") @RequestParam("q") String q,
            @Parameter(description = "Maximum number of results") @RequestParam(name = "limit", defaultValue = "5") int limit) {
        List<SearchResultDto> results = recommendationService.searchByQuery(q, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Item-to-item recommendations by article ID.
     * GET /api/v1/recommendations/{articleId}
     */
    @GetMapping("/{articleId}")
    @Operation(summary = "Item-to-item recommendations", description = "Find articles similar to the given article ID")
    public ResponseEntity<RecommendationDto> getRecommendations(
            @Parameter(description = "Source article ID") @PathVariable("articleId") String articleId,
            @Parameter(description = "Maximum number of recommendations") @RequestParam(name = "limit", defaultValue = "5") int limit) {
        RecommendationDto recommendations = recommendationService.findSimilarByArticleId(articleId, limit);
        return ResponseEntity.ok(recommendations);
    }
}