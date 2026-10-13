package com.company.recsys.repository;

import com.company.recsys.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for Article documents.
 * Provides projection support for optimized Smart Tips queries.
 */
@Repository
public interface ArticleRepository extends MongoRepository<Article, String> {

    /**
     * Find article by ID with full document.
     */
    Optional<Article> findById(String id);

    /**
     * Find multiple articles by IDs (for batch fetching after vector search).
     */
    List<Article> findAllById(Iterable<String> ids);

    /**
     * Projection interface for Smart Tips - only headline and seoLocation.
     * This avoids fetching the full document (including large synopsis) for lightweight endpoints.
     */
    interface SmartTipsProjection {
        String getId();
        String getHeadline();
        String getSeoLocation();
    }

    /**
     * Find article by ID with Smart Tips projection (optimized).
     * Returns only id, headline, and seoLocation fields.
     */
    @Query(value = "{ '_id': ?0 }", fields = "{ 'hl': 1, 'seolocation': 1 }")
    Optional<SmartTipsProjection> findSmartTipsById(String id);

    /**
     * Projection interface for Summarization - only headline and synopsis.
     */
    interface SummaryProjection {
        String getId();
        String getHeadline();
        String getSynopsis();
    }

    /**
     * Find article by ID with Summary projection.
     * Returns only id, headline, and synopsis fields.
     */
    @Query(value = "{ '_id': ?0 }", fields = "{ 'hl': 1, 'syn': 1 }")
    Optional<SummaryProjection> findSummaryById(String id);

    /**
     * Check if article exists (lightweight).
     */
    boolean existsById(String id);

    /**
     * Count total articles.
     */
    long count();

    /**
     * Find articles with pagination (for admin/debugging).
     */
    Page<Article> findAll(Pageable pageable);
}