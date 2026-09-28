# Paper-Pilot Backend Detailed Documentation

## 1. Architecture Overview

Paper-Pilot backend follows a layered architecture:

1. **Controller layer** exposes HTTP APIs and wraps responses into `ApiFromResponse<T>`.
2. **Service layer** handles document lifecycle, ingestion, retrieval, and LLM orchestration.
3. **Persistence layer** stores document metadata in PostgreSQL (`documents`) and vectors in pgvector (`vector_store`).
4. **AI integration layer** uses Spring AI ChatClient and OpenAI-compatible APIs (OpenRouter base URL).

---

## 2. Request Flow

### Upload and Index

1. `DocumentController.uploadDocument` receives multipart file.
2. `DocumentProcessingService.UploadAndProcessDocument` creates a document metadata row with `UPLOADING`.
3. `ParseDocumentService` extracts raw content:
   - PDF via `PagePdfDocumentReader`
   - Generic docs via `TikaDocumentReader`
4. `InjestionService.ingestDocument`:
   - marks status `PROCESSING`
   - splits text with `TokenTextSplitter`
   - enriches each chunk with metadata (`documentId`, `filename`, `chunkIndex`, `pageNumber`, etc.)
   - stores chunks in vector store
   - marks status `INDEXED`

### Chat / RAG

1. `ChatController.generate-response` accepts `UserInputDto`.
2. `RetrievalAugmentedGenerationService.retrieveRelevantInformation` runs similarity search (optional document filter).
3. Retrieved chunks are converted into:
   - **context block** for LLM prompt
   - **citations** for frontend display
4. Service calls model with grounded prompt and returns `ResponseDto`.

### Streamed Chat

- `stream-generate-response` follows same retrieval path but streams text output.

---

## 3. Data Models

### `Document` entity

- `id` (UUID)
- `filename`
- `contentType`
- `fileSize`
- `totalPages`
- `totalChunks`
- `status` (`UPLOADING`, `PROCESSING`, `INDEXED`, `FAILED`, etc.)
- `errorMessage`
- `createdAt`, `updatedAt`

### DTOs

- `DocumentResponseDTO`: upload/index result summary
- `DocMetaDTO`: document metadata list/detail response
- `UserInputDto`: query parameters for retrieval/chat
- `ResponseDto`: generated answer + citations + response time
- `CitationDto`: doc-level traceability for chunk provenance

---

## 4. Retrieval & Metadata Details

To ensure reliable filtered retrieval:

- Chunk metadata stores:
  - `documentId` as string UUID
  - `filename` and `fileName` for compatibility
- Retrieval applies metadata filter on `documentId` when provided.
- If strict filtered search returns empty, fallback search attempts broader retrieval and then in-memory document-id matching.

This avoids false zero-result cases due to metadata representation differences.

---

## 5. Prompting Strategy

The backend constructs a **grounded user prompt** that includes:

- retrieved context snippets with source markers
- explicit instruction to answer based on provided context
- instruction to acknowledge missing context when insufficient

This prevents the model from replying as if no attachment exists when relevant context was retrieved.

---

## 6. Timeout Resilience

Provider/network instability can surface as `OpenAIInvalidDataException` with timeout causes.

Current safeguards:

1. Context size cap via `app.rag.max-context-chars`
2. Retry with compact context (`app.rag.retry-context-chars`) when timeout-like failure occurs
3. Dedicated `AiServiceException` mapped to:
   - `504 Gateway Timeout` for timeout scenarios
   - `502 Bad Gateway` for provider failures

---

## 7. Configuration Reference

`application-dev.yml` key groups:

- `spring.datasource.*` - PostgreSQL connection
- `spring.ai.openai.*` - model provider endpoint and key
- `spring.ai.vectorstore.pgvector.*` - vector table/schema/index config
- `app.rag.*` - retrieval/chunk/prompt constraints
- `app.cors.*` - CORS policy

Key note:

- `remove-existing-vector-store-table: false` keeps vectors across restarts.

---

## 8. API Response Shape

Most API endpoints return:

```json
{
  "success": true,
  "message": "text",
  "data": {},
  "timestamp": "ISO-8601"
}
```

Streaming endpoint returns chunked text stream directly.

---

## 9. Operational Checklist

Before testing RAG:

1. Confirm DB is reachable and pgvector is enabled.
2. Upload document and verify status is `INDEXED`.
3. Query with `documentId` set (for document-scoped chat).
4. Inspect logs:
   - retrieval count
   - timeout retries
   - ingestion status

---

## 10. Troubleshooting Guide

### Issue: `Retrieved 0 documents`

Check:

- document indexed successfully
- `documentId` in request matches indexed document metadata
- similarity threshold not too strict

### Issue: model says no document attached

Check:

- retrieval returned > 0 chunks
- context assembly uses correct metadata keys (`filename`, `pageNumber`)

### Issue: timeout / `Error reading response`

Check:

- model/provider health
- context size (reduce topK/chunk size or use compact retry)
- network latency and API limits

---

## 11. Suggested Next Hardening

- Add per-request correlation IDs for end-to-end tracing.
- Add dedicated health endpoint for vector store readiness.
- Add integration tests covering:
  - upload → index → retrieve by documentId
  - timeout retry path
  - citation metadata mapping correctness
