package com.company.recsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main Spring Boot application for the AI-Powered Article Recommendation System.
 * RAG Pipeline: CSV → MongoDB (raw) + MariaDB Vector Store (embeddings)
 * 4 REST endpoints: Semantic Search, Item-to-Item, Smart Tips, AI Summarization
 */
@SpringBootApplication
@EnableCaching
public class RecommendationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationSystemApplication.class, args);
    }
}