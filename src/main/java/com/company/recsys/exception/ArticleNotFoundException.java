package com.company.recsys.exception;

/**
 * Exception thrown when an article is not found in the database.
 */
public class ArticleNotFoundException extends RuntimeException {

    private final String articleId;

    public ArticleNotFoundException(String articleId) {
        super("Article not found with id: " + cleanId(articleId));
        this.articleId = cleanId(articleId);
    }

    private static String cleanId(String id) {
        if (id == null) return "";
        if (id.contains("ID:")) {
            return id.substring(id.lastIndexOf("ID:") + 3).trim();
        }
        if (id.contains("id:")) {
            return id.substring(id.lastIndexOf("id:") + 3).trim();
        }
        return id.trim();
    }

    public String getArticleId() {
        return articleId;
    }
}