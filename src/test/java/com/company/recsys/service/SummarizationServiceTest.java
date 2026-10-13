package com.company.recsys.service;

import com.company.recsys.dto.SummaryDto;
import com.company.recsys.exception.ArticleNotFoundException;
import com.company.recsys.rag.PromptTemplates;
import com.company.recsys.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SummarizationServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ChatClient groqChatClient;

    @Mock
    private ChatClient openRouterChatClient;

    @Mock
    private PromptTemplates promptTemplates;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SummarizationService summarizationService;

    @BeforeEach
    void setUp() {
        summarizationService = new SummarizationService(
                articleRepository,
                groqChatClient,
                openRouterChatClient,
                promptTemplates
        );
    }

    @Test
    void summarize_whenArticleNotFound_throwsException() {
        given(articleRepository.findSummaryById("999")).willReturn(Optional.empty());

        assertThatThrownBy(() -> summarizationService.summarize("999"))
                .isInstanceOf(ArticleNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void summarize_whenArticleExists_callsGroqChatClient() {
        ArticleRepository.SummaryProjection projection = new ArticleRepository.SummaryProjection() {
            @Override
            public String getId() { return "101"; }
            @Override
            public String getHeadline() { return "AI Trends"; }
            @Override
            public String getSynopsis() { return "Detailed content about AI"; }
        };

        given(articleRepository.findSummaryById("101")).willReturn(Optional.of(projection));
        given(promptTemplates.createSummaryPrompt("AI Trends", "Detailed content about AI"))
                .willReturn(new Prompt("Test prompt"));

        given(groqChatClient.prompt(any(Prompt.class))).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.content()).willReturn("- Point 1\n- Point 2\n- Point 3");

        SummaryDto result = summarizationService.summarize("101");

        assertThat(result.getArticleId()).isEqualTo("101");
        assertThat(result.getHeadline()).isEqualTo("AI Trends");
        assertThat(result.getSummary()).isEqualTo("- Point 1\n- Point 2\n- Point 3");
    }

    @Test
    void summarize_whenGroqFails_fallsBackGracefully() {
        ArticleRepository.SummaryProjection projection = new ArticleRepository.SummaryProjection() {
            @Override
            public String getId() { return "102"; }
            @Override
            public String getHeadline() { return "Health Benefits of Apples"; }
            @Override
            public String getSynopsis() {
                return "Apples are nutritious and packed with fiber and vitamin C. Eating apples regularly supports healthy gut bacteria and digestion. They may also lower the risk of chronic conditions like heart disease.";
            }
        };

        given(articleRepository.findSummaryById("102")).willReturn(Optional.of(projection));
        given(promptTemplates.createSummaryPrompt(any(), any()))
                .willReturn(new Prompt("Test prompt"));

        // Groq fails with an exception
        given(groqChatClient.prompt(any(Prompt.class))).willThrow(new RuntimeException("Groq 404 model not found"));

        SummaryDto result = summarizationService.summarize("102");

        assertThat(result.getArticleId()).isEqualTo("102");
        assertThat(result.getHeadline()).isEqualTo("Health Benefits of Apples");
        // Extractive fallback should produce a 3-bullet summary
        assertThat(result.getSummary()).contains("- ");
        assertThat(result.getSummary().split("\n")).hasSize(3);
    }
}
