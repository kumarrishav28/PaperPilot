# Paper-Pilot Backend

Spring Boot backend for Paper-Pilot RAG workflows: document ingestion, vector retrieval, and AI response generation.

## What This Service Does

- Accepts document uploads
- Parses PDF and non-PDF documents
- Splits content into chunks and embeds them
- Stores vectors in PostgreSQL pgvector
- Retrieves top-k relevant chunks for a query
- Generates grounded LLM responses and citations

## Tech Stack

- Java + Spring Boot
- Spring AI (OpenAI-compatible API + pgvector store)
- PostgreSQL + pgvector
- Maven

## Project Layout

| Area | Path |
|---|---|
| Controllers | `src/main/java/com/PaperPilot/controller` |
| Services | `src/main/java/com/PaperPilot/service` |
| DTOs | `src/main/java/com/PaperPilot/dto` |
| Entities/Repository | `src/main/java/com/PaperPilot/entity`, `repository` |
| Config | `src/main/java/com/PaperPilot/config` |
| Runtime config | `src/main/resources/application*.yml` |

## API Endpoints

### Documents

- `POST /api/documents/upload` (multipart key: `document`)
- `POST /api/documents/upload/multiple` (multipart key: `documents`)
- `GET /api/documents/all`
- `GET /api/documents/fetch/{documentId}`
- `DELETE /api/documents/delete/{documentId}`

### Chat

- `POST /api/chat/generate-response`
- `POST /api/chat/stream-generate-response`
- `POST /api/chat/search-similar`

`UserInputDto` payload:

```json
{
  "query": "what is this document about?",
  "documentId": "uuid-optional",
  "topK": 5,
  "similarityThreshold": 0.0,
  "conversationId": "optional"
}
```

## Environment & Configuration

Primary config: `src/main/resources/application-dev.yml`

Important env vars:

- `OPENROUTER_API_KEY`
- `OPENAI_CHAT_MODEL` (defaults to `openrouter/free`)
- `OPENAI_CHAT_TEMPERATURE`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`

Important RAG params:

- `app.rag.top-k`
- `app.rag.similarity-threshold`
- `app.rag.max-context-chars`
- `app.rag.retry-context-chars`

## Run Locally

1. Ensure PostgreSQL with `vector` extension is available.
2. Set environment variables.
3. Start app:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Deep Documentation

For full architecture, ingestion and retrieval internals, failure modes, and troubleshooting:

`docs/BACKEND-DETAILED.md`
