package com.company.recsys.rag;

import com.company.recsys.model.Article;
import com.company.recsys.repository.ArticleRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ingestion Pipeline - Phase 1 of RAG (TRD Section 4.1).
 * Reads CSV data using OpenCSV → Creates Spring AI Documents → Generates embeddings →
 * Saves raw rows to MongoDB and vector embeddings to MariaDB Vector Store.
 */
@Component
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final ArticleRepository articleRepository;
    private final VectorStore vectorStore;

    @Value("${ingestion.csv.batch-size:100}")
    private int batchSize = 100;

    @Value("${ingestion.csv.embedding-batch-size:32}")
    private int embeddingBatchSize = 32;

    public IngestionPipeline(ArticleRepository articleRepository,
                             VectorStore vectorStore) {
        this.articleRepository = articleRepository;
        this.vectorStore = vectorStore;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setEmbeddingBatchSize(int embeddingBatchSize) {
        this.embeddingBatchSize = embeddingBatchSize;
    }

    /**
     * Ingest articles from a CSV file using OpenCSV.
     * Processes in batches for memory efficiency.
     */
    public IngestionResult ingestCsv(Resource csvResource) {
        log.info("Starting CSV ingestion from resource: {}", csvResource != null ? csvResource.getDescription() : "null");

        long startTime = System.currentTimeMillis();
        int totalProcessed = 0;
        int totalSaved = 0;
        int totalErrors = 0;
        List<String> errors = new ArrayList<>();

        if (csvResource == null || !csvResource.exists()) {
            String err = "CSV resource does not exist or is null";
            log.error(err);
            return new IngestionResult(0, 0, 1, List.of(err), System.currentTimeMillis() - startTime);
        }

        try (InputStreamReader isr = new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(isr)) {

            String[] header = csvReader.readNext();
            if (header == null) {
                return new IngestionResult(0, 0, 0, List.of("Empty CSV file"), System.currentTimeMillis() - startTime);
            }

            Map<String, Integer> colIndexMap = buildColumnMap(header);

            List<Article> articleBatch = new ArrayList<>();
            List<Document> documentBatch = new ArrayList<>();
            int effectiveBatchSize = batchSize > 0 ? batchSize : 100;

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                totalProcessed++;

                try {
                    Article article = parseRow(row, colIndexMap);
                    if (article != null) {
                        articleBatch.add(article);

                        String embeddingText = article.getEmbeddingText();
                        if (StringUtils.hasText(embeddingText)) {
                            Document doc = new Document(embeddingText);
                            doc.getMetadata().put("articleId", article.getId());
                            doc.getMetadata().put("headline", article.getHeadline());
                            documentBatch.add(doc);
                        }

                        if (articleBatch.size() >= effectiveBatchSize) {
                            int saved = processBatch(articleBatch, documentBatch);
                            totalSaved += saved;
                            articleBatch.clear();
                            documentBatch.clear();
                        }
                    }
                } catch (Exception e) {
                    totalErrors++;
                    errors.add("Row " + totalProcessed + ": " + e.getMessage());
                    log.warn("Error processing row {}: {}", totalProcessed, e.getMessage());
                }
            }

            if (!articleBatch.isEmpty()) {
                int saved = processBatch(articleBatch, documentBatch);
                totalSaved += saved;
            }

        } catch (IOException | CsvValidationException e) {
            log.error("Fatal error during CSV ingestion: ", e);
            errors.add("Fatal error: " + e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Ingestion completed. Processed: {}, Saved: {}, Errors: {}, Duration: {}ms",
                totalProcessed, totalSaved, totalErrors, duration);

        return new IngestionResult(totalProcessed, totalSaved, totalErrors, errors, duration);
    }

    private Map<String, Integer> buildColumnMap(String[] header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            map.put(header[i].trim().toLowerCase(), i);
        }
        return map;
    }

    private Article parseRow(String[] row, Map<String, Integer> map) {
        String id = getVal(row, map, "id");
        if (!StringUtils.hasText(id)) {
            return null;
        }

        String createdAtStr = getVal(row, map, "createdat");
        String dl = getVal(row, map, "dl");
        String hl = getVal(row, map, "hl");
        String imageid = getVal(row, map, "imageid");
        String lu = getVal(row, map, "lu");
        String seolocation = getVal(row, map, "seolocation");
        String syn = getVal(row, map, "syn");

        Instant createdAt = parseInstant(createdAtStr);

        return new Article(id, createdAt, dl, hl, imageid, lu, seolocation, syn);
    }

    private String getVal(String[] row, Map<String, Integer> map, String key) {
        Integer idx = map.get(key);
        if (idx == null || idx >= row.length) {
            return "";
        }
        return row[idx] != null ? row[idx].trim() : "";
    }

    private Instant parseInstant(String timestampStr) {
        if (!StringUtils.hasText(timestampStr)) {
            return Instant.now();
        }
        try {
            return Instant.parse(timestampStr);
        } catch (DateTimeParseException e) {
            try {
                return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(timestampStr));
            } catch (DateTimeParseException ex) {
                return Instant.now();
            }
        }
    }

    private int processBatch(List<Article> articles, List<Document> documents) {
        if (articles.isEmpty()) {
            return 0;
        }

        List<Article> savedArticles = articleRepository.saveAll(articles);

        if (!documents.isEmpty()) {
            int step = embeddingBatchSize > 0 ? embeddingBatchSize : 32;
            for (int i = 0; i < documents.size(); i += step) {
                int end = Math.min(i + step, documents.size());
                List<Document> embeddingBatch = documents.subList(i, end);
                try {
                    vectorStore.add(embeddingBatch);
                } catch (Exception e) {
                    log.error("Error storing embeddings batch: ", e);
                    throw new RuntimeException("Failed to store embeddings", e);
                }
            }
        }

        return savedArticles.size();
    }

    public static class IngestionResult {
        private final int totalProcessed;
        private final int totalSaved;
        private final int totalErrors;
        private final List<String> errors;
        private final long durationMs;

        public IngestionResult(int totalProcessed, int totalSaved, int totalErrors,
                               List<String> errors, long durationMs) {
            this.totalProcessed = totalProcessed;
            this.totalSaved = totalSaved;
            this.totalErrors = totalErrors;
            this.errors = errors;
            this.durationMs = durationMs;
        }

        public int getTotalProcessed() { return totalProcessed; }
        public int getTotalSaved() { return totalSaved; }
        public int getTotalErrors() { return totalErrors; }
        public List<String> getErrors() { return errors; }
        public long getDurationMs() { return durationMs; }

        public boolean isSuccess() {
            return totalErrors == 0 && totalSaved > 0;
        }
    }
}