import { AnimatePresence, motion } from 'framer-motion'
import { Eraser, Menu, Paperclip, SendHorizonal } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import { SelectedDocumentChip } from './Sidebar'

function TypingIndicator() {
  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="max-w-[70%] rounded-2xl border border-slate-800 bg-slate-900 px-4 py-3">
      <div className="flex items-center gap-1.5">
        {[0, 1, 2].map((dot) => (
          <motion.span
            key={dot}
            className="h-2 w-2 rounded-full bg-cyan-400"
            animate={{ y: [0, -4, 0], opacity: [0.4, 1, 0.4] }}
            transition={{ repeat: Number.POSITIVE_INFINITY, delay: dot * 0.12, duration: 0.8 }}
          />
        ))}
      </div>
    </motion.div>
  )
}

function CitationTag({ citation }) {
  return (
    <div className="group relative inline-flex">
      <button
        type="button"
        className="rounded-md border border-cyan-500/40 bg-cyan-500/10 px-2 py-1 text-[11px] text-cyan-300 transition hover:bg-cyan-500/20"
      >
        [Doc: {citation.filename} • Chunk {citation.chunkIndex ?? '—'}]
      </button>
      <div className="pointer-events-none absolute -top-2 left-0 z-20 hidden w-80 -translate-y-full rounded-lg border border-slate-700 bg-slate-900 p-3 text-xs text-slate-300 shadow-xl group-hover:block">
        <p className="mb-1 text-slate-100">Context preview</p>
        <p className="line-clamp-5">{citation.snippet || 'No snippet available.'}</p>
        <p className="mt-2 text-[10px] text-slate-500">Page: {citation.pageNumber ?? '—'} • Similarity: {citation.similarityScore?.toFixed?.(3) ?? '—'}</p>
      </div>
    </div>
  )
}

function MessageBubble({ message }) {
  const isUser = message.role === 'user'
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}
    >
      <div
        className={`max-w-[80%] rounded-2xl px-4 py-3 text-sm ${
          isUser
            ? 'bg-gradient-to-r from-violet-600 to-indigo-600 text-white'
            : 'border border-slate-800 bg-slate-900 text-slate-200'
        }`}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <div className="max-w-none space-y-2 text-slate-100 [&_code]:rounded [&_code]:bg-slate-950 [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:text-cyan-300">
            <ReactMarkdown>{message.content}</ReactMarkdown>
          </div>
        )}
        {!isUser && message.citations?.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-2">
            {message.citations.map((citation, idx) => (
              <CitationTag key={`${citation.documentId}-${idx}`} citation={citation} />
            ))}
          </div>
        )}
      </div>
    </motion.div>
  )
}

export default function ChatPanel({
  selectedDocument,
  messages,
  pending,
  streamMode,
  onToggleStreamMode,
  onSend,
  onClearHistory,
  onOpenMobileSidebar,
}) {
  const [input, setInput] = useState('')
  const threadRef = useRef(null)

  useEffect(() => {
    if (threadRef.current) {
      threadRef.current.scrollTop = threadRef.current.scrollHeight
    }
  }, [messages, pending])

  const conversationId = useMemo(() => {
    const aiMessages = messages.filter((item) => item.role === 'assistant')
    return aiMessages[aiMessages.length - 1]?.conversationId || 'new-session'
  }, [messages])

  const handleSubmit = (event) => {
    event.preventDefault()
    const query = input.trim()
    if (!query || pending) return
    onSend(query)
    setInput('')
  }

  return (
    <div className="relative flex min-h-screen flex-1 flex-col">
      <div className="sticky top-0 z-10 border-b border-slate-800/80 bg-slate-950/80 px-4 py-3 backdrop-blur lg:px-6">
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={onOpenMobileSidebar}
            className="rounded-md border border-slate-700 p-2 text-slate-300 hover:bg-slate-800 xl:hidden"
          >
            <Menu className="h-4 w-4" />
          </button>

          <div className="min-w-0 flex-1">
            <h2 className="truncate text-sm font-semibold text-slate-100">RAG Session</h2>
            <p className="text-xs text-slate-400">Conversation: {conversationId}</p>
          </div>

          <label className="inline-flex items-center gap-2 text-xs text-slate-300">
            <input type="checkbox" checked={streamMode} onChange={onToggleStreamMode} className="accent-violet-500" />
            Stream mode
          </label>

          <button
            type="button"
            onClick={onClearHistory}
            className="inline-flex items-center gap-2 rounded-md border border-slate-700 px-2.5 py-1.5 text-xs text-slate-300 hover:bg-slate-800"
          >
            <Eraser className="h-3.5 w-3.5" />
            Clear history
          </button>

          <SelectedDocumentChip filename={selectedDocument?.filename} />
        </div>
      </div>

      <div ref={threadRef} className="flex-1 space-y-4 overflow-y-auto px-4 py-6 lg:px-6">
        {messages.length === 0 && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mx-auto max-w-xl rounded-2xl border border-slate-800 bg-slate-900/60 p-6 text-center">
            <h3 className="text-lg font-semibold text-slate-100">Welcome to Paper-Pilot</h3>
            <p className="mt-2 text-sm text-slate-400">Upload a paper, select it, and ask deep questions with retrieval-grounded answers.</p>
          </motion.div>
        )}
        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} />
        ))}
        <AnimatePresence>{pending && <TypingIndicator />}</AnimatePresence>
      </div>

      <div className="sticky bottom-0 z-20 border-t border-slate-800/80 bg-slate-950/80 p-4 backdrop-blur lg:px-6">
        <form onSubmit={handleSubmit} className="mx-auto flex max-w-4xl items-end gap-2 rounded-2xl border border-slate-800 bg-slate-900 p-2 shadow-glow">
          <button
            type="button"
            className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-800 hover:text-cyan-300"
            title="Attach selected document context"
          >
            <Paperclip className="h-4 w-4" />
          </button>
          <textarea
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask anything about your selected documents..."
            className="max-h-40 min-h-[44px] flex-1 resize-y rounded-lg border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-100 outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-500/40"
          />
          <button
            type="submit"
            disabled={!input.trim() || pending}
            className="inline-flex items-center gap-2 rounded-lg bg-gradient-to-r from-violet-600 to-indigo-600 px-3 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <SendHorizonal className="h-4 w-4" />
            Send
          </button>
        </form>
      </div>
    </div>
  )
}
