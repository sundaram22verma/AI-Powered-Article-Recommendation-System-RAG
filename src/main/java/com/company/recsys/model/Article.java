package com.company.recsys.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * MongoDB document representing an article.
 * Mirrors the raw CSV/Excel row - no fields are dropped at ingestion.
 */
@Document(collection = "articles")
public class Article {

    @Id
    private String id;

    @Field("createdAt")
    private Instant createdAt;

    @Field("dl")
    private String dl;

    @Field("hl")
    private String headline;

    @Field("imageid")
    private String imageId;

    @Field("lu")
    private String lu;

    @Field("seolocation")
    private String seoLocation;

    @Field("syn")
    private String synopsis;

    // Default constructor for MongoDB
    public Article() {}

    // All-args constructor
    public Article(String id, Instant createdAt, String dl, String headline,
                   String imageId, String lu, String seoLocation, String synopsis) {
        this.id = id;
        this.createdAt = createdAt;
        this.dl = dl;
        this.headline = headline;
        this.imageId = imageId;
        this.lu = lu;
        this.seoLocation = seoLocation;
        this.synopsis = synopsis;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getDl() {
        return dl;
    }

    public void setDl(String dl) {
        this.dl = dl;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getLu() {
        return lu;
    }

    public void setLu(String lu) {
        this.lu = lu;
    }

    public String getSeoLocation() {
        return seoLocation;
    }

    public void setSeoLocation(String seoLocation) {
        this.seoLocation = seoLocation;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    /**
     * Returns the concatenated text used for embedding generation.
     * Combines headline and synopsis as specified in TRD Section 4.1.
     */
    public String getEmbeddingText() {
        StringBuilder sb = new StringBuilder();
        if (headline != null && !headline.isBlank()) {
            sb.append(headline).append(". ");
        }
        if (synopsis != null && !synopsis.isBlank()) {
            sb.append(synopsis);
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return "Article{" +
                "id='" + id + '\'' +
                ", headline='" + headline + '\'' +
                ", seoLocation='" + seoLocation + '\'' +
                '}';
    }
}