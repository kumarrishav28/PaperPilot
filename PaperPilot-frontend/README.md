# Paper-Pilot Frontend

Modern React SPA for the Paper-Pilot AI RAG application, using Tailwind CSS, Framer Motion, and Lucide icons.

## Features

- JWT-style local session management (with configurable real auth endpoints)
- Document upload workspace with drag/drop and upload progress
- Indexed document listing with status badges and delete controls
- RAG chat with markdown responses and citation tags
- Optional stream mode for progressive AI output
- Responsive two-column layout with mobile sidebar

## Stack

- React (Vite)
- Tailwind CSS
- Framer Motion
- Lucide React
- React Markdown

## Run

```bash
npm install
npm run dev
```

## Build

```bash
npm run build
npm run preview
```

## Environment

Copy `.env.example` to `.env` and adjust:

- `VITE_API_BASE_URL` - backend base URL.
- `VITE_AUTH_LOGIN_ENDPOINT` and `VITE_AUTH_REGISTER_ENDPOINT` - optional auth endpoints. If omitted, the app uses mock local authentication with JWT-like local tokens.

## Backend API Contract Implemented

### Documents

- `POST /api/documents/upload`
- `POST /api/documents/upload/multiple`
- `GET /api/documents/all`
- `GET /api/documents/fetch/{documentId}`
- `DELETE /api/documents/delete/{documentId}`

### Chat

- `POST /api/chat/generate-response`
- `POST /api/chat/stream-generate-response`
- `POST /api/chat/search-similar`

## Detailed Documentation

For architecture, component-level behavior, UX patterns, and extension guidance:

`docs/FRONTEND-DETAILED.md`
