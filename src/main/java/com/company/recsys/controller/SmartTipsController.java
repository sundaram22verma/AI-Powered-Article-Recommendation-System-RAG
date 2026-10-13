package com.company.recsys.controller;

import com.company.recsys.dto.SmartTipsDto;
import com.company.recsys.service.SmartTipsService;
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
 * Controller for Smart Tips endpoint.
 * Lightweight endpoint returning headline and link for relevant articles.
 */
@RestController
@RequestMapping("/api/v1/articles")
@Tag(name = "Smart Tips", description = "Lightweight article tips with headline and link")
public class SmartTipsController {

    private final SmartTipsService smartTipsService;

    public SmartTipsController(SmartTipsService smartTipsService) {
        this.smartTipsService = smartTipsService;
    }

    /**
     * Get smart tips for an article by ID.
     * GET /api/v1/articles/{articleId}/smart-tips?limit=5
     */
    @GetMapping("/{articleId}/smart-tips")
    @Operation(summary = "Get smart tips", description = "Get lightweight article recommendations (headline and link only) for relevant articles")
    public ResponseEntity<List<SmartTipsDto>> getSmartTips(
            @Parameter(description = "Article ID") @PathVariable("articleId") String articleId,
            @Parameter(description = "Maximum number of smart tips to return") @RequestParam(name = "limit", defaultValue = "5") int limit) {
        List<SmartTipsDto> smartTips = smartTipsService.getSmartTips(articleId, limit);
        return ResponseEntity.ok(smartTips);
    }
}