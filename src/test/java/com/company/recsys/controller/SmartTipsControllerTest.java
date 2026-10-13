package com.company.recsys.controller;

import com.company.recsys.dto.SmartTipsDto;
import com.company.recsys.exception.ArticleNotFoundException;
import com.company.recsys.service.SmartTipsService;
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

@WebMvcTest(SmartTipsController.class)
class SmartTipsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmartTipsService smartTipsService;

    @Test
    void getSmartTips_shouldReturnRelevantArticles() throws Exception {
        SmartTipsDto dto1 = new SmartTipsDto("456", "Related Article 1", "http://example.com/art1");
        SmartTipsDto dto2 = new SmartTipsDto("789", "Related Article 2", "http://example.com/art2");
        given(smartTipsService.getSmartTips("123", 5)).willReturn(java.util.List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/articles/123/smart-tips")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value("456"))
                .andExpect(jsonPath("$[0].headline").value("Related Article 1"))
                .andExpect(jsonPath("$[0].seoLocation").value("http://example.com/art1"))
                .andExpect(jsonPath("$[1].articleId").value("789"));
    }

    @Test
    void getSmartTips_whenNotFound_shouldReturn404() throws Exception {
        given(smartTipsService.getSmartTips("999", 5)).willThrow(new ArticleNotFoundException("999"));

        mockMvc.perform(get("/api/v1/articles/999/smart-tips")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("999"));
    }
}
