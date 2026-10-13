package com.company.recsys.rag;

import com.company.recsys.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IngestionPipelineTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private VectorStore vectorStore;

    private IngestionPipeline ingestionPipeline;

    @BeforeEach
    void setUp() {
        ingestionPipeline = new IngestionPipeline(articleRepository, vectorStore);
    }

    @Test
    void ingestCsv_withValidData_parsesAndSaves() {
        String csvData = """
                id,createdAt,dl,hl,imageid,lu,seolocation,syn
                101,2025-01-01T10:00:00Z,dl,Sample Headline,img101,lu,http://example.com/sample,Sample Synopsis
                """;

        ByteArrayResource resource = new ByteArrayResource(csvData.getBytes(StandardCharsets.UTF_8));
        given(articleRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        IngestionPipeline.IngestionResult result = ingestionPipeline.ingestCsv(resource);

        assertThat(result.getTotalProcessed()).isEqualTo(1);
        assertThat(result.getTotalSaved()).isEqualTo(1);
        assertThat(result.getTotalErrors()).isEqualTo(0);
        assertThat(result.isSuccess()).isTrue();
    }
}
