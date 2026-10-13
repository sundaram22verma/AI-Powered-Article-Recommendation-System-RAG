package com.company.recsys.config;

import com.company.recsys.rag.IngestionPipeline;
import com.company.recsys.repository.ArticleRepository;
import com.company.recsys.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Startup Data Initializer.
 * Automatically ingests raw CSV article data into MongoDB and vector embeddings into MariaDB VectorStore
 * on application startup if the database is empty or auto-bootstrap is enabled.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ArticleRepository articleRepository;
    private final IngestionService ingestionService;

    @Value("${ingestion.auto-bootstrap:true}")
    private boolean autoBootstrap;

    @Value("${ingestion.csv-location:file:consolidated_data.csv}")
    private Resource csvResource;

    public DataInitializer(ArticleRepository articleRepository,
                           IngestionService ingestionService) {
        this.articleRepository = articleRepository;
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(String... args) {
        if (!autoBootstrap) {
            log.info("Startup auto-ingestion is disabled by configuration (ingestion.auto-bootstrap=false)");
            return;
        }

        long existingCount = articleRepository.count();
        if (existingCount > 0) {
            log.info("MongoDB already contains {} articles. Skipping startup CSV ingestion.", existingCount);
            return;
        }

        log.info("MongoDB is empty. Starting automatic CSV ingestion from {}...", csvResource.getDescription());
        try {
            IngestionPipeline.IngestionResult result = ingestionService.ingestCsv(csvResource);
            log.info("Startup CSV ingestion completed successfully. Saved {} articles in {} ms (Errors: {}).",
                    result.getTotalSaved(), result.getDurationMs(), result.getTotalErrors());
        } catch (Exception e) {
            log.error("Failed to perform automatic startup CSV ingestion: ", e);
        }
    }
}
