package com.company.recsys.controller;

import com.company.recsys.dto.RecommendationDto;
import com.company.recsys.dto.SearchResultDto;
import com.company.recsys.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @Test
    void searchByQuery_shouldReturnMatchingArticles() throws Exception {
        SearchResultDto dto = new SearchResultDto("123", "Spring AI Guide", "http://example.com/spring-ai", "Synopsis body", 0.95);
        given(recommendationService.searchByQuery(eq("spring ai"), anyInt())).willReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/recommendations/search")
                        .param("q", "spring ai")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value("123"))
                .andExpect(jsonPath("$[0].headline").value("Spring AI Guide"))
                .andExpect(jsonPath("$[0].similarityScore").value(0.95));
    }

    @Test
    void findSimilarByArticleId_shouldReturnRecommendations() throws Exception {
        SearchResultDto reco = new SearchResultDto("456", "Related Topic", "http://example.com/related", "Synopsis body 2", 0.88);
        RecommendationDto dto = new RecommendationDto("123", List.of(reco));
        given(recommendationService.findSimilarByArticleId(eq("123"), anyInt())).willReturn(dto);

        mockMvc.perform(get("/api/v1/recommendations/123")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceArticleId").value("123"))
                .andExpect(jsonPath("$.recommendations[0].articleId").value("456"));
    }
}
