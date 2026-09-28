# PaperPilot

PaperPilot is a full-stack Retrieval-Augmented Generation (RAG) application for uploading documents, indexing them into a vector store, and chatting with AI grounded on retrieved document chunks.

## Repository Structure

| Module | Path | Purpose |
|---|---|---|
| Frontend SPA | `PaperPilot-frontend/` | React + Tailwind single-page app for document workspace and RAG chat |
| Backend API | `PaperPilot-backend/` | Spring Boot API for upload, parsing, ingestion, vector retrieval, and AI responses |

## Documentation

- Frontend overview and setup: `PaperPilot-frontend/README.md`
- Frontend deep-dive: `PaperPilot-frontend/docs/FRONTEND-DETAILED.md`
- Backend overview and setup: `PaperPilot-backend/README.md`
- Backend deep-dive: `PaperPilot-backend/docs/BACKEND-DETAILED.md`

## Quick Start

1. Start backend (`PaperPilot-backend`) with PostgreSQL + pgvector available.
2. Start frontend (`PaperPilot-frontend`) and open the app in browser.
3. Upload a document, wait until status becomes **Indexed**, then query via chat.

## Core Features

- Document upload (single and multiple files)
- Chunking and embedding into pgvector-backed vector store
- Similarity search with optional per-document filtering
- Chat completion and streaming completion APIs
- Citation metadata mapped from retrieval results
- Sleek, responsive UI with dark theme and animated interactions
# PaperPilot
<img width="1707" height="1430" alt="image" src="https://github.com/user-attachments/assets/3145b737-b4ef-4329-81b2-2354c18c7182" />
