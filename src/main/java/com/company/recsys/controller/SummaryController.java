package com.company.recsys.controller;

import com.company.recsys.dto.SummaryDto;
import com.company.recsys.service.SummarizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for AI-powered article summarization.
 * Uses Groq (primary) with OpenRouter fallback.
 */
@RestController
@RequestMapping("/api/v1/articles")
@Tag(name = "Summarization", description = "AI-powered article summarization with caching")
public class SummaryController {

    private final SummarizationService summarizationService;

    public SummaryController(SummarizationService summarizationService) {
        this.summarizationService = summarizationService;
    }

    /**
     * Get AI-generated summary for an article.
     * GET /api/v1/articles/{articleId}/summary
     * Results are cached to reduce LLM API calls.
     */
    @GetMapping("/{articleId}/summary")
    @Operation(summary = "Get article summary", description = "Get 3-bullet-point AI summary with Groq/OpenRouter fallback")
    public ResponseEntity<SummaryDto> getSummary(
            @Parameter(description = "Article ID") @PathVariable("articleId") String articleId) {
        SummaryDto summary = summarizationService.summarize(articleId);
        return ResponseEntity.ok(summary);
    }
}