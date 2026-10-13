package com.company.recsys.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI Configuration.
 * Access UI at http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Powered Article Recommendation System API")
                        .version("1.0.0")
                        .description("RAG-based Article Recommendation, Smart Tips, and AI Summarization Service using Spring AI, MongoDB, MariaDB Vector Store, Groq, and OpenRouter.")
                        .contact(new Contact()
                                .name("Recommendation Engine Team")
                                .email("dev@company.com")));
    }
}
