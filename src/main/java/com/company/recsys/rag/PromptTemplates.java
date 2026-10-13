package com.company.recsys.rag;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Prompt Templates for RAG operations (TRD Section 4.2).
 */
@Component
public class PromptTemplates {

    private final Resource summarizeTemplateResource;

    public PromptTemplates(@Value("classpath:prompts/summarize-template.st") Resource summarizeTemplateResource) {
        this.summarizeTemplateResource = summarizeTemplateResource;
    }

    /**
     * Create prompt for article summarization.
     */
    public Prompt createSummaryPrompt(String headline, String content) {
        if (summarizeTemplateResource != null && summarizeTemplateResource.exists()) {
            PromptTemplate template = new PromptTemplate(summarizeTemplateResource);
            return template.create(Map.of(
                    "headline", headline != null ? headline : "",
                    "content", content != null ? content : ""
            ));
        }

        // Fallback default inline prompt template
        PromptTemplate fallbackTemplate = new PromptTemplate("""
                You are a helpful assistant. Summarize the following article concisely in 3 bullet points.
                Headline: {headline}
                Content: {content}
                """);
        return fallbackTemplate.create(Map.of(
                "headline", headline != null ? headline : "",
                "content", content != null ? content : ""
        ));
    }
}