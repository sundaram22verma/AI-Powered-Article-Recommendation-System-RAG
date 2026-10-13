package com.company.recsys.service;

import com.company.recsys.dto.RecommendationDto;
import com.company.recsys.dto.SearchResultDto;
import com.company.recsys.model.Article;
import com.company.recsys.rag.RetrievalPipeline;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for recommendation operations.
 * Handles semantic search and item-to-item recommendations.
 */
@Service
public class RecommendationService {

    private final RetrievalPipeline retrievalPipeline;

    public RecommendationService(RetrievalPipeline retrievalPipeline) {
        this.retrievalPipeline = retrievalPipeline;
    }

    /**
     * Semantic search by query text.
     */
    public List<SearchResultDto> searchByQuery(String query, int limit) {
        List<RetrievalPipeline.ScoredArticle> scoredArticles = retrievalPipeline.searchByQuery(query, limit);
        return scoredArticles.stream()
                .map(this::toSearchResultDto)
                .collect(Collectors.toList());
    }

    /**
     * Item-to-item recommendations by article ID.
     */
    public RecommendationDto findSimilarByArticleId(String articleId, int limit) {
        List<RetrievalPipeline.ScoredArticle> scoredArticles = retrievalPipeline.findSimilarByArticleId(articleId, limit);
        List<SearchResultDto> recommendations = scoredArticles.stream()
                .map(this::toSearchResultDto)
                .collect(Collectors.toList());

        return new RecommendationDto(articleId, recommendations);
    }

    private SearchResultDto toSearchResultDto(RetrievalPipeline.ScoredArticle scoredArticle) {
        Article article = scoredArticle.getArticle();
        SearchResultDto dto = new SearchResultDto();
        dto.setArticleId(article.getId());
        dto.setHeadline(article.getHeadline());
        dto.setSeoLocation(article.getSeoLocation());
        dto.setSynopsis(article.getSynopsis());
        dto.setSimilarityScore(scoredArticle.getScore());
        return dto;
    }
}