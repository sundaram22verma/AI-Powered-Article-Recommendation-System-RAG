package com.company.recsys.service;

import com.company.recsys.dto.RecommendationDto;
import com.company.recsys.dto.SearchResultDto;
import com.company.recsys.model.Article;
import com.company.recsys.rag.RetrievalPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RetrievalPipeline retrievalPipeline;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(retrievalPipeline);
    }

    @Test
    void searchByQuery_returnsMappedSearchResults() {
        Article article = new Article("101", Instant.now(), "dl", "Headline 101", "img", "lu", "http://seo.com", "Synopsis");
        RetrievalPipeline.ScoredArticle scored = new RetrievalPipeline.ScoredArticle(article, 0.92);

        given(retrievalPipeline.searchByQuery("tech", 5)).willReturn(List.of(scored));

        List<SearchResultDto> results = recommendationService.searchByQuery("tech", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getArticleId()).isEqualTo("101");
        assertThat(results.get(0).getHeadline()).isEqualTo("Headline 101");
        assertThat(results.get(0).getSimilarityScore()).isEqualTo(0.92);
    }

    @Test
    void findSimilarByArticleId_returnsSimilarArticles() {
        Article article = new Article("102", Instant.now(), "dl", "Similar Article", "img", "lu", "http://seo.com/2", "Synopsis 2");
        RetrievalPipeline.ScoredArticle scored = new RetrievalPipeline.ScoredArticle(article, 0.85);

        given(retrievalPipeline.findSimilarByArticleId("101", 3)).willReturn(List.of(scored));

        RecommendationDto result = recommendationService.findSimilarByArticleId("101", 3);

        assertThat(result.getSourceArticleId()).isEqualTo("101");
        assertThat(result.getRecommendations()).hasSize(1);
        assertThat(result.getRecommendations().get(0).getArticleId()).isEqualTo("102");
    }
}
