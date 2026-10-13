# AI-Powered Article Recommendation System (RAG)

[![License: All Rights Reserved](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

An enterprise AI-driven recommendation and summarization system built with **Spring Boot 3**, **Spring AI**, **MongoDB**, **MariaDB Vector Store**, and **LLMs (Groq & OpenRouter)**.

The system ingests raw article data from `consolidated_data.csv`, generates vector embeddings locally using an ONNX Sentence Transformer model (`all-MiniLM-L6-v2`), and serves semantic search recommendations, item-to-item related content, lightweight smart tips, and AI article summaries.

---

## 🏗️ Architecture Overview

```
                          ┌───────────────────────────┐
                          │  consolidated_data.csv    │
                          └─────────────┬─────────────┘
                                        │ (Startup Auto-Ingestion)
                                        ▼
                          ┌───────────────────────────┐
                          │   Spring Boot Application │
                          └──────┬─────────────┬──────┘
                                 │             │
            (Full Raw Documents) │             │ (384-dim Embeddings)
                                 ▼             ▼
                          ┌───────────┐   ┌──────────────────────────┐
                          │  MongoDB  │   │   MariaDB Vector Store   │
                          └───────────┘   └──────────────────────────┘
                                 ▲             ▲
                                 │             │
                ┌────────────────┴─────────────┴────────────────┐
                │             RAG Retrieval & Services          │
                └──────┬──────────────┬──────────────┬──────────┘
                       │              │              │
                       ▼              ▼              ▼
                Semantic Search  Item-to-Item    Smart Tips
                (/recommendations /recommendations (/articles/
                  /search)          /{id})       {id}/smart-tips)
                       │
                       │ (Summarization Request)
                       ▼
                ┌───────────────────────────────────────────────┐
                │ Groq LLM Client (llama-3.3-70b-versatile)     │
                │ Fallback: OpenRouter (llama-3.3-70b-instruct) │
                └──────────────────────┬────────────────────────┘
                                       │
                                       ▼
                               Article Summary
                               (/articles/{id}/summary)
```

---

## 🛠️ Technology Stack

- **Java Version**: 21
- **Framework**: Spring Boot 3.3.3
- **AI Integration**: Spring AI 1.0.0-M1 (RAG, Prompt Templates, Chat Clients)
- **Primary Data Store**: MongoDB 7.0 (stores raw article records as-is)
- **Vector Database**: MariaDB 11.7 (vector search with HNSW vector index)
- **Embedding Model**: Local ONNX `all-MiniLM-L6-v2` (384 dimensions)
- **LLM Providers**: Groq API (Primary) & OpenRouter API (Fallback)
- **Fault Tolerance**: Resilience4j (CircuitBreaker, Retry, TimeLimiter)
- **Caching**: Spring Cache (`@Cacheable` for LLM summaries)
- **API Documentation**: SpringDoc OpenAPI / Swagger UI

---

## 🚀 Key Endpoints

Base path: `/api/v1`

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/recommendations/search?q={query}&limit={n}` | `GET` | Semantic search against MariaDB Vector Store + Mongo doc fetch |
| `/recommendations/{articleId}?limit={n}` | `GET` | Item-to-item KNN recommendations for a given article |
| `/articles/{articleId}/smart-tips` | `GET` | Fast UI tooltip metadata (`headline`, `seoLocation`) via Mongo projection |
| `/articles/{articleId}/summary` | `GET` | 3-bullet point AI summary using Groq with OpenRouter fallback |
| `/swagger-ui.html` | `GET` | Interactive Swagger UI API documentation |
| `/actuator/health` | `GET` | Application & Database health check |

---

## 📦 Data Storage & Ingestion Strategy

1. **MongoDB**: Stores all raw fields from `consolidated_data.csv` (`id`, `createdAt`, `dl`, `hl`, `imageid`, `lu`, `seolocation`, `syn`) verbatim.
2. **MariaDB Vector Store**: Stores only document vectors (384-dim) and `articleId` references in table `recsys_vectors.article_embeddings`.
3. **Startup Auto-Ingestion**: On application boot, `DataInitializer` checks if MongoDB is empty. If empty, it automatically reads `consolidated_data.csv` via OpenCSV and populates MongoDB + MariaDB Vector Store without requiring manual API calls.

---

## ⚙️ Setup & Execution

### 1. Environment Variables Setup
Copy `.env.example` to `.env` and fill in your Groq & OpenRouter API keys:
```bash
cp .env.example .env
```

### 2. Run with Docker Compose
```bash
docker-compose up -d --build
```
This starts MongoDB, MariaDB (with vector support), and the Spring Boot application container.

### 3. Local Development Run
Start MongoDB and MariaDB via Docker:
```bash
docker-compose up -d mongodb mariadb
```
Then run the Spring Boot app:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Running Unit & Integration Tests
```bash
mvn clean test
```

---

## 📄 License

**Copyright © 2026. All Rights Reserved.**

This project was developed as an **internship project** and is published solely for **personal portfolio and reference purposes**.

- **No Modifications Allowed:** Nobody is permitted to modify, alter, or create derivative works from this codebase.
- **No Redistribution or Commercial Use:** Copying, redistributing, publishing, sub-licensing, or deploying this project in any form without prior written authorization is strictly prohibited.

For complete terms and legal restrictions, see the [`LICENSE`](LICENSE) file.
