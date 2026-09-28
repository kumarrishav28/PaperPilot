# Paper-Pilot Frontend Detailed Documentation

## 1. Architecture Overview

The frontend is a React SPA (Vite) designed around two primary states:

1. **Unauthenticated state** - tabbed Login/Register modal.
2. **Authenticated state** - dashboard with document workspace + chat interface.

Primary technologies:

- React
- Tailwind CSS
- Framer Motion
- Lucide React
- React Markdown

---

## 2. Folder Structure

| Path | Purpose |
|---|---|
| `src/App.jsx` | App orchestration, state transitions, API workflow |
| `src/components/AuthModal.jsx` | Login/Register UI with validation |
| `src/components/Sidebar.jsx` | Upload, document list, workspace switch, profile/logout |
| `src/components/ChatPanel.jsx` | Message thread, citations, input composer, stream mode |
| `src/context/AuthContext.jsx` | JWT-like session storage and auth state |
| `src/services/api.js` | API client for backend contracts |
| `src/index.css` | Tailwind directives + global theme rules |

---

## 3. Visual System

Design palette implementation:

- Background: `bg-slate-950`
- Card/surfaces: `bg-slate-900/80`, `border-slate-800/80`
- CTA gradient: violet/indigo
- Highlight accents: cyan
- Primary text: slate-100
- Muted text: slate-400

Interactions:

- Motion transitions for auth, sidebar, and message entries
- Typing indicator animation while waiting
- Mobile collapsible sidebar with overlay backdrop

---

## 4. Authentication Flow

`AuthContext` stores:

- token: `paper-pilot-token`
- user: `paper-pilot-user`

Behavior:

- On success, session is persisted in localStorage.
- On logout, local state and storage are cleared.
- If auth endpoints are not configured, app uses local mock auth fallback.

---

## 5. API Integration

Implemented backend endpoints:

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

Upload progress:

- Single-file upload uses `XMLHttpRequest` to expose progress events.
- Multi-file upload uses `FormData`.

---

## 6. Chat & RAG UX Behavior

When user sends a message:

1. User message is appended to thread.
2. Payload includes:
   - query
   - selected `documentId`
   - `topK`
   - `similarityThreshold`
   - latest `conversationId` when available
3. If stream mode is off:
   - call `generate-response`
   - render markdown response + citations
4. If stream mode is on:
   - call `stream-generate-response`
   - progressively update assistant message

Citation tags display source chunk details with hover previews.

---

## 7. Environment Configuration

Use `.env` (copy from `.env.example`):

- `VITE_API_BASE_URL` (backend base URL)
- `VITE_AUTH_LOGIN_ENDPOINT` (optional)
- `VITE_AUTH_REGISTER_ENDPOINT` (optional)

Vite dev proxy is configured for `/api` → `http://localhost:8080`.

---

## 8. Build and Run

```bash
npm install
npm run dev
```

Production build:

```bash
npm run build
npm run preview
```

---

## 9. Error Handling Strategy

- UI-level fallback assistant message is shown when upload/chat fails.
- Auth form validates and shows field-level + server-level messages.
- Upload and delete failures do not crash app state.

---

## 10. Extension Guide

Recommended next additions:

- Real backend auth integration with refresh token handling.
- Conversation history persistence per workspace.
- Optimistic UI updates with background refresh.
- Accessibility pass (ARIA live regions for streaming, keyboard refinements).
- Unit/component tests for upload/chat flows.
