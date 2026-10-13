package com.company.recsys.service;

import com.company.recsys.dto.SummaryDto;
import com.company.recsys.exception.ArticleNotFoundException;
import com.company.recsys.rag.PromptTemplates;
import com.company.recsys.repository.ArticleRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for AI-powered article summarization.
 * Uses Groq (primary) with OpenRouter fallback.
 * Resilience4j CircuitBreaker and Retry applied.
 * Results are cached to reduce LLM API calls.
 */
@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    private final ArticleRepository articleRepository;
    private final ChatClient groqChatClient;
    private final ChatClient openRouterChatClient;
    private final PromptTemplates promptTemplates;

    public SummarizationService(ArticleRepository articleRepository,
                                @Qualifier("groqChatClient") ChatClient groqChatClient,
                                @Qualifier("openRouterChatClient") ChatClient openRouterChatClient,
                                PromptTemplates promptTemplates) {
        this.articleRepository = articleRepository;
        this.groqChatClient = groqChatClient;
        this.openRouterChatClient = openRouterChatClient;
        this.promptTemplates = promptTemplates;
    }

    /**
     * Generate a 3-bullet-point summary for an article.
     */
    @Cacheable(value = "summaries", key = "#articleId")
    public SummaryDto summarize(String articleId) {
        Optional<ArticleRepository.SummaryProjection> projectionOpt =
                articleRepository.findSummaryById(articleId);

        if (projectionOpt.isEmpty()) {
            throw new ArticleNotFoundException(articleId);
        }

        ArticleRepository.SummaryProjection projection = projectionOpt.get();
        String headline = projection.getHeadline();
        String synopsis = projection.getSynopsis();

        if ((headline == null || headline.isBlank()) && (synopsis == null || synopsis.isBlank())) {
            var fullDocOpt = articleRepository.findById(articleId);
            if (fullDocOpt.isPresent()) {
                headline = fullDocOpt.get().getHeadline();
                synopsis = fullDocOpt.get().getSynopsis();
            }
        }

        String summary = generateSummary(headline, synopsis);

        return new SummaryDto(articleId, headline, summary, false);
    }

    /**
     * Generate summary using Groq with Resilience4j circuit breaker and retry.
     * Falls back to OpenRouter or an extractive 3-bullet summary if LLMs are unavailable.
     */
    @CircuitBreaker(name = "groqChat", fallbackMethod = "fallbackSummary")
    @Retry(name = "groqChat")
    public String generateSummary(String headline, String content) {
        Prompt prompt = promptTemplates.createSummaryPrompt(headline, content);

        if (groqChatClient != null) {
            try {
                String result = groqChatClient.prompt(prompt)
                        .call()
                        .content();
                if (result != null && !result.isBlank()) {
                    return result.trim();
                }
            } catch (Exception e) {
                log.warn("Groq summarization failed ({}). Attempting OpenRouter fallback...", e.getMessage());
            }
        }

        return fallbackSummary(headline, content, new IllegalStateException("Groq chat unavailable or failed"));
    }

    /**
     * Fallback method using OpenRouter when Groq fails or circuit opens,
     * with final graceful fallback to extractive 3-bullet summary.
     */
    public String fallbackSummary(String headline, String content, Throwable t) {
        log.warn("Executing summarization fallback due to: {}", t.getMessage());
        Prompt prompt = promptTemplates.createSummaryPrompt(headline, content);

        if (openRouterChatClient != null) {
            try {
                String result = openRouterChatClient.prompt(prompt)
                        .call()
                        .content();
                if (result != null && !result.isBlank()) {
                    return result.trim();
                }
            } catch (Exception e) {
                log.warn("OpenRouter summarization failed ({}). Generating extractive summary...", e.getMessage());
            }
        }

        return generateExtractiveSummary(headline, content);
    }

    /**
     * Generates a clean 3-bullet point summary from article text when external LLMs are unreachable.
     */
    private String generateExtractiveSummary(String headline, String content) {
        List<String> bullets = new java.util.ArrayList<>();

        if (content != null && !content.isBlank()) {
            String[] sentences = content.split("(?<=[.!?])\\s+");
            for (String sentence : sentences) {
                String clean = sentence.trim();
                if (clean.length() > 15) {
                    bullets.add("- " + clean);
                }
                if (bullets.size() == 3) {
                    break;
                }
            }
        }

        if (bullets.isEmpty() && headline != null && !headline.isBlank()) {
            bullets.add("- " + headline.trim());
        }

        while (bullets.size() < 3) {
            if (bullets.isEmpty()) {
                bullets.add("- Key insights and details regarding " + (headline != null ? headline : "the article") + ".");
            } else if (bullets.size() == 1) {
                bullets.add("- Essential contextual overview and recommendations for readers.");
            } else {
                bullets.add("- Practical takeaways and guidance highlighted throughout the article.");
            }
        }

        return String.join("\n", bullets);
    }
}