package com.company.recsys.controller;

import com.company.recsys.dto.SummaryDto;
import com.company.recsys.exception.ArticleNotFoundException;
import com.company.recsys.service.SummarizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SummaryController.class)
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummarizationService summarizationService;

    @Test
    void summarizeArticle_shouldReturnSummary() throws Exception {
        SummaryDto dto = new SummaryDto("123", "Spring AI Intro", "- Point 1\n- Point 2\n- Point 3", false);
        given(summarizationService.summarize("123")).willReturn(dto);

        mockMvc.perform(get("/api/v1/articles/123/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articleId").value("123"))
                .andExpect(jsonPath("$.headline").value("Spring AI Intro"))
                .andExpect(jsonPath("$.summary").value("- Point 1\n- Point 2\n- Point 3"));
    }

    @Test
    void summarizeArticle_whenNotFound_shouldReturn404() throws Exception {
        given(summarizationService.summarize("999")).willThrow(new ArticleNotFoundException("999"));

        mockMvc.perform(get("/api/v1/articles/999/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("999"));
    }
}
