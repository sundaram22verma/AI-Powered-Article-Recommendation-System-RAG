package com.company.recsys.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

/**
 * Spring AI Configuration for Chat and Embedding models.
 * Configures Groq (primary), OpenRouter (fallback), and local Transformers embeddings.
 * Updated for Spring AI 1.1.0-M1-PLATFORM-2 API changes.
 */
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.chat.base-url:https://api.groq.com/openai}")
    private String groqBaseUrl = "https://api.groq.com/openai";

    @Value("${spring.ai.openai.chat.api-key:}")
    private String groqApiKey = "";

    @Value("${spring.ai.openai.chat.model:openai/gpt-oss-20b}")
    private String groqModel = "openai/gpt-oss-20b";

    @Value("${spring.ai.openrouter.chat.base-url:https://openrouter.ai/api}")
    private String openRouterBaseUrl = "https://openrouter.ai/api";

    @Value("${spring.ai.openrouter.chat.api-key:}")
    private String openRouterApiKey = "";

    @Value("${spring.ai.openrouter.chat.model:meta-llama/llama-3.3-70b-instruct}")
    private String openRouterModel = "meta-llama/llama-3.3-70b-instruct";

    @Value("${spring.ai.model.onnx.embedding.model-path:}")
    private Resource embeddingModelPath;

    @Value("${spring.ai.model.onnx.embedding.tokenizer-path:}")
    private Resource embeddingTokenizerPath;

    private String normalizeBaseUrl(String url) {
        if (url == null) return "";
        url = url.trim();
        if (url.endsWith("/v1")) {
            url = url.substring(0, url.length() - 3);
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * Primary Chat Model - Groq (fast, free tier).
     * Used for article summarization.
     */
    @Bean
    @Primary
    public ChatModel groqChatModel() {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            return null;
        }

        OpenAiApi groqApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(groqBaseUrl))
                .apiKey(groqApiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(groqModel)
                .temperature(0.3)
                .maxTokens(500)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(groqApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * Primary Chat Client - Groq.
     */
    @Bean
    @Primary
    public ChatClient groqChatClient(ChatModel groqChatModel) {
        if (groqChatModel == null) {
            return null;
        }
        return ChatClient.builder(groqChatModel).build();
    }

    /**
     * Fallback Chat Model - OpenRouter.
     * Used when Groq is unavailable.
     */
    @Bean
    public ChatModel openRouterChatModel() {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            // Return null if not configured - services should handle this
            return null;
        }

        OpenAiApi openRouterApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(openRouterBaseUrl))
                .apiKey(openRouterApiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(openRouterModel)
                .temperature(0.3)
                .maxTokens(500)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openRouterApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * Fallback Chat Client - OpenRouter.
     */
    @Bean
    public ChatClient openRouterChatClient(ChatModel openRouterChatModel) {
        if (openRouterChatModel == null) {
            return null;
        }
        return ChatClient.builder(openRouterChatModel).build();
    }

    /**
     * Local Embedding Model using Transformers (Sentence Transformers ONNX).
     * Runs locally without API calls - cost effective for ingestion.
     */
    @Bean
    @Primary
    public EmbeddingModel localEmbeddingModel() {
        if (embeddingModelPath == null || !embeddingModelPath.exists()) {
            throw new IllegalStateException("EMBEDDING_MODEL_PATH is not configured or file does not exist. Please set it in environment variables.");
        }

        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        model.setModelResource(embeddingModelPath);
        model.setTokenizerResource(embeddingTokenizerPath);
        return model;
    }
}