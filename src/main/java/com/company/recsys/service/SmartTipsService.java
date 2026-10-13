package com.company.recsys.service;

import com.company.recsys.dto.SmartTipsDto;
import com.company.recsys.exception.ArticleNotFoundException;
import com.company.recsys.model.Article;
import com.company.recsys.rag.RetrievalPipeline;
import com.company.recsys.repository.ArticleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for Smart Tips - lightweight endpoint returning headline and link for relevant articles.
 * Excludes the requested article itself and returns up to 'limit' unique recommended articles.
 */
@Service
public class SmartTipsService {

    private final ArticleRepository articleRepository;
    private final RetrievalPipeline retrievalPipeline;

    public SmartTipsService(ArticleRepository articleRepository,
                            RetrievalPipeline retrievalPipeline) {
        this.articleRepository = articleRepository;
        this.retrievalPipeline = retrievalPipeline;
    }

    /**
     * Get smart tips (relevant recommended articles) for an article by ID.
     * Excludes the source article, prevents duplicates, and respects the limit parameter.
     */
    public List<SmartTipsDto> getSmartTips(String articleId, int limit) {
        if (!articleRepository.existsById(articleId)) {
            throw new ArticleNotFoundException(articleId);
        }

        int effectiveLimit = limit > 0 ? limit : 5;
        List<RetrievalPipeline.ScoredArticle> scoredArticles =
                retrievalPipeline.findSimilarByArticleId(articleId, effectiveLimit);

        Set<String> seenIds = new LinkedHashSet<>();
        List<SmartTipsDto> smartTips = new ArrayList<>();

        for (RetrievalPipeline.ScoredArticle scored : scoredArticles) {
            Article article = scored.getArticle();
            if (article != null && article.getId() != null) {
                String id = article.getId();
                if (!id.equals(articleId) && seenIds.add(id)) {
                    smartTips.add(new SmartTipsDto(
                            article.getId(),
                            article.getHeadline(),
                            article.getSeoLocation()
                    ));
                    if (smartTips.size() >= effectiveLimit) {
                        break;
                    }
                }
            }
        }

        return smartTips;
    }

    /**
     * Overload for backward compatibility defaulting to 5 recommendations.
     */
    public List<SmartTipsDto> getSmartTips(String articleId) {
        return getSmartTips(articleId, 5);
    }
}