package com.company.recsys.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * MariaDB Vector Store Configuration.
 * Uses Spring AI's MariaDB VectorStore implementation.
 */
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.mariadb.initialize-schema:true}")
    private boolean initializeSchema;

    @Value("${spring.ai.vectorstore.mariadb.schema-name:recsys_vectors}")
    private String schemaName;

    @Value("${spring.ai.vectorstore.mariadb.table-name:article_embeddings}")
    private String tableName;

    @Value("${spring.ai.vectorstore.mariadb.dimensions:384}")
    private int dimensions;

    @Value("${spring.ai.vectorstore.mariadb.distance-type:COSINE}")
    private String distanceType;

    /**
     * MariaDB Vector Store bean.
     * Stores article embeddings with articleId as metadata for retrieval.
     */
    @Bean
    @Primary
    public VectorStore mariaDbVectorStore(DataSource dataSource, EmbeddingModel embeddingModel) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Initialize schema if needed
        if (initializeSchema) {
            initializeVectorSchema(jdbcTemplate);
        }

        return MariaDBVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(schemaName)
                .vectorTableName(tableName)
                .dimensions(dimensions)
                .distanceType(MariaDBVectorStore.MariaDBDistanceType.valueOf(distanceType))
                .build();
    }

    /**
     * Initialize the vector store schema with proper indexes.
     */
    private void initializeVectorSchema(JdbcTemplate jdbcTemplate) {
        // Create schema if not exists
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        // Create vector table with VECTOR column compatible with Spring AI MariaDBVectorStore
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s.%s (
                id VARCHAR(36) PRIMARY KEY,
                content LONGTEXT,
                metadata JSON,
                embedding VECTOR(%d) NOT NULL
            )
            """, schemaName, tableName, dimensions);

        jdbcTemplate.execute(createTableSql);
    }
}