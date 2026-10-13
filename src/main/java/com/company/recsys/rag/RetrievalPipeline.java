package com.company.recsys.rag;

import com.company.recsys.model.Article;
import com.company.recsys.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Retrieval Pipeline - Phase 2 of RAG (TRD Section 4.2).
 * Handles semantic search and item-to-item recommendations via vector similarity.
 */
@Component
public class RetrievalPipeline {

    private static final Logger log = LoggerFactory.getLogger(RetrievalPipeline.class);

    private final VectorStore vectorStore;
    private final ArticleRepository articleRepository;

    public RetrievalPipeline(VectorStore vectorStore,
                             ArticleRepository articleRepository) {
        this.vectorStore = vectorStore;
        this.articleRepository = articleRepository;
    }

    /**
     * Semantic search by query text (TRD Section 5.1).
     * Converts query to vector → searches MariaDB vector store → fetches full docs from MongoDB.
     */
    public List<ScoredArticle> searchByQuery(String query, int limit) {
        log.debug("Semantic search for query: '{}' with limit {}", query, limit);

        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .build();

            List<Document> similarDocs = vectorStore.similaritySearch(request);

            if (similarDocs.isEmpty()) {
                log.debug("No similar documents found for query: {}", query);
                return List.of();
            }

            return mapDocumentsToScoredArticles(similarDocs);

        } catch (Exception e) {
            log.error("Error during semantic search for query: {}", query, e);
            throw new RuntimeException("Semantic search failed", e);
        }
    }

    /**
     * Item-to-item recommendations by article ID (TRD Section 5.2).
     * Finds articles similar to the given article using its embedding.
     */
    public List<ScoredArticle> findSimilarByArticleId(String articleId, int limit) {
        log.debug("Finding similar articles for articleId: {} with limit {}", articleId, limit);

        if (!articleRepository.existsById(articleId)) {
            log.warn("Article not found for similar search: {}", articleId);
            return List.of();
        }

        try {
            var articleOpt = articleRepository.findById(articleId);
            if (articleOpt.isEmpty()) {
                return List.of();
            }

            Article sourceArticle = articleOpt.get();
            String queryText = sourceArticle.getEmbeddingText();

            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression filter = builder.ne("articleId", articleId).build();

            SearchRequest request = SearchRequest.builder()
                    .query(queryText)
                    .topK(Math.max(limit * 2, limit + 10))
                    .filterExpression(filter)
                    .build();

            List<Document> similarDocs = vectorStore.similaritySearch(request);

            java.util.Set<String> seenIds = new java.util.LinkedHashSet<>();
            List<Document> filteredDocs = similarDocs.stream()
                    .filter(doc -> {
                        Object idObj = doc.getMetadata().get("articleId");
                        if (idObj instanceof String id && !id.equals(articleId)) {
                            return seenIds.add(id);
                        }
                        return false;
                    })
                    .limit(limit)
                    .toList();

            return mapDocumentsToScoredArticles(filteredDocs);

        } catch (Exception e) {
            log.error("Error finding similar articles for: {}", articleId, e);
            throw new RuntimeException("Similar article search failed", e);
        }
    }

    private List<ScoredArticle> mapDocumentsToScoredArticles(List<Document> docs) {
        List<String> articleIds = docs.stream()
                .map(doc -> doc.getMetadata().get("articleId"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .toList();

        if (articleIds.isEmpty()) {
            return List.of();
        }

        List<Article> articles = articleRepository.findAllById(articleIds);
        Map<String, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, Function.identity(), (a, b) -> a));

        List<ScoredArticle> result = new ArrayList<>();
        java.util.Set<String> addedIds = new java.util.HashSet<>();
        for (Document doc : docs) {
            Object idObj = doc.getMetadata().get("articleId");
            if (idObj != null && articleMap.containsKey(idObj.toString())) {
                String id = idObj.toString();
                if (addedIds.add(id)) {
                    Article article = articleMap.get(id);
                    Double score = doc.getScore();
                    result.add(new ScoredArticle(article, score));
                }
            }
        }
        return result;
    }

    public static class ScoredArticle {
        private final Article article;
        private final Double score;

        public ScoredArticle(Article article, Double score) {
            this.article = article;
            this.score = score;
        }

        public Article getArticle() { return article; }
        public Double getScore() { return score; }
    }
}