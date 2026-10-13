package com.company.recsys.service;

import com.company.recsys.dto.SmartTipsDto;
import com.company.recsys.exception.ArticleNotFoundException;
import com.company.recsys.model.Article;
import com.company.recsys.rag.RetrievalPipeline;
import com.company.recsys.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SmartTipsServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private RetrievalPipeline retrievalPipeline;

    private SmartTipsService smartTipsService;

    @BeforeEach
    void setUp() {
        smartTipsService = new SmartTipsService(articleRepository, retrievalPipeline);
    }

    @Test
    void getSmartTips_whenArticleExists_returnsRelevantArticlesExcludingSource() {
        String sourceId = "101";
        given(articleRepository.existsById(sourceId)).willReturn(true);

        Article article1 = new Article("102", Instant.now(), "dl", "Healthy Food", "img", "lu", "http://test.com/102", "Syn 1");
        Article article2 = new Article("103", Instant.now(), "dl", "Exercise Tips", "img", "lu", "http://test.com/103", "Syn 2");
        Article sourceAgain = new Article("101", Instant.now(), "dl", "Source Headline", "img", "lu", "http://test.com/101", "Syn Source");

        given(retrievalPipeline.findSimilarByArticleId(sourceId, 5)).willReturn(List.of(
                new RetrievalPipeline.ScoredArticle(article1, 0.95),
                new RetrievalPipeline.ScoredArticle(sourceAgain, 0.99), // Should be excluded!
                new RetrievalPipeline.ScoredArticle(article2, 0.90)
        ));

        List<SmartTipsDto> result = smartTipsService.getSmartTips(sourceId, 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getArticleId()).isEqualTo("102");
        assertThat(result.get(0).getHeadline()).isEqualTo("Healthy Food");
        assertThat(result.get(1).getArticleId()).isEqualTo("103");
        // Verify source article 101 is NOT in results
        assertThat(result).noneMatch(tip -> tip.getArticleId().equals(sourceId));
    }

    @Test
    void getSmartTips_deduplicatesArticles() {
        String sourceId = "101";
        given(articleRepository.existsById(sourceId)).willReturn(true);

        Article article1 = new Article("102", Instant.now(), "dl", "Headline 1", "img", "lu", "http://test.com/102", "Syn 1");
        Article article1Duplicate = new Article("102", Instant.now(), "dl", "Headline 1", "img", "lu", "http://test.com/102", "Syn 1");

        given(retrievalPipeline.findSimilarByArticleId(sourceId, 5)).willReturn(List.of(
                new RetrievalPipeline.ScoredArticle(article1, 0.95),
                new RetrievalPipeline.ScoredArticle(article1Duplicate, 0.94)
        ));

        List<SmartTipsDto> result = smartTipsService.getSmartTips(sourceId, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArticleId()).isEqualTo("102");
    }

    @Test
    void getSmartTips_respectsLimitParameter() {
        String sourceId = "101";
        given(articleRepository.existsById(sourceId)).willReturn(true);

        Article article1 = new Article("102", Instant.now(), "dl", "H1", "img", "lu", "http://test.com/1", "Syn 1");
        Article article2 = new Article("103", Instant.now(), "dl", "H2", "img", "lu", "http://test.com/2", "Syn 2");
        Article article3 = new Article("104", Instant.now(), "dl", "H3", "img", "lu", "http://test.com/3", "Syn 3");

        given(retrievalPipeline.findSimilarByArticleId(sourceId, 2)).willReturn(List.of(
                new RetrievalPipeline.ScoredArticle(article1, 0.9),
                new RetrievalPipeline.ScoredArticle(article2, 0.8),
                new RetrievalPipeline.ScoredArticle(article3, 0.7)
        ));

        List<SmartTipsDto> result = smartTipsService.getSmartTips(sourceId, 2);

        assertThat(result).hasSize(2);
    }

    @Test
    void getSmartTips_whenArticleNotFound_throwsException() {
        given(articleRepository.existsById("999")).willReturn(false);

        assertThatThrownBy(() -> smartTipsService.getSmartTips("999", 5))
                .isInstanceOf(ArticleNotFoundException.class)
                .hasMessageContaining("999");
    }
}
