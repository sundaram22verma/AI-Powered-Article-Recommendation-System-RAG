package com.company.recsys.service;

import com.company.recsys.rag.IngestionPipeline;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Service for CSV ingestion operations.
 */
@Service
public class IngestionService {

    private final IngestionPipeline ingestionPipeline;

    public IngestionService(IngestionPipeline ingestionPipeline) {
        this.ingestionPipeline = ingestionPipeline;
    }

    /**
     * Ingest articles from a CSV resource.
     */
    public IngestionPipeline.IngestionResult ingestCsv(Resource csvResource) {
        return ingestionPipeline.ingestCsv(csvResource);
    }
}