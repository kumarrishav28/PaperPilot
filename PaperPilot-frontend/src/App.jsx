import { AnimatePresence, motion } from 'framer-motion'
import { useEffect, useMemo, useState } from 'react'
import AuthModal from './components/AuthModal'
import ChatPanel from './components/ChatPanel'
import Sidebar from './components/Sidebar'
import { useAuth } from './context/AuthContext'
import {
  deleteDocument,
  generateResponse,
  getAllDocuments,
  getDocumentById,
  searchSimilar,
  streamGenerateResponse,
  uploadDocument,
  uploadMultipleDocuments,
} from './services/api'

function normalizeDocument(raw) {
  return {
    id: raw.id || raw.documentId,
    filename: raw.filename || raw.documentName,
    fileSize: raw.fileSize || 0,
    status: raw.status || raw.documentStatus || 'PROCESSING',
  }
}

function App() {
  const { isAuthenticated, logout, user } = useAuth()
  const [documents, setDocuments] = useState([])
  const [selectedDocumentId, setSelectedDocumentId] = useState('')
  const [messages, setMessages] = useState([])
  const [pending, setPending] = useState(false)
  const [streamMode, setStreamMode] = useState(false)
  const [workspace, setWorkspace] = useState('research')
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false)
  const [uploadState, setUploadState] = useState({
    active: false,
    progress: 0,
    fileName: '',
  })

  const selectedDocument = useMemo(
    () => documents.find((doc) => doc.id === selectedDocumentId) || null,
    [documents, selectedDocumentId],
  )

  useEffect(() => {
    if (!isAuthenticated) return
    let alive = true
    getAllDocuments()
      .then((res) => {
        if (!alive) return
        const docs = (res?.data || []).map(normalizeDocument)
        setDocuments(docs)
        setSelectedDocumentId((prev) => prev || docs[0]?.id || '')
      })
      .catch(() => {})
    return () => {
      alive = false
    }
  }, [isAuthenticated])

  const pushUserMessage = (query) => {
    const id = crypto.randomUUID()
    setMessages((prev) => [...prev, { id, role: 'user', content: query }])
  }

  const pushAssistantMessage = (payload) => {
    const id = crypto.randomUUID()
    setMessages((prev) => [...prev, { id, role: 'assistant', ...payload }])
  }

  const handleUploadFiles = async (files) => {
    if (!files.length) return
    setUploadState({ active: true, progress: 0, fileName: files[0].name })

    try {
      if (files.length === 1) {
        const response = await uploadDocument(files[0], (progress) => {
          setUploadState((prev) => ({ ...prev, progress }))
        })
        const uploaded = normalizeDocument(response?.data || {})
        let latest = uploaded
        if (uploaded.id) {
          const fetched = await getDocumentById(uploaded.id).catch(() => null)
          if (fetched?.data) {
            latest = normalizeDocument(fetched.data)
          }
        }
        setDocuments((prev) => [latest, ...prev.filter((doc) => doc.id !== latest.id)])
        setSelectedDocumentId(latest.id)
      } else {
        setUploadState((prev) => ({ ...prev, progress: 40 }))
        const response = await uploadMultipleDocuments(files)
        setUploadState((prev) => ({ ...prev, progress: 90 }))
        const uploadedDocs = (response?.data || []).map(normalizeDocument)
        setDocuments((prev) => [...uploadedDocs, ...prev])
        if (uploadedDocs[0]?.id) setSelectedDocumentId(uploadedDocs[0].id)
      }
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: 'I could not upload the document(s). Please verify backend connectivity and file format.',
          citations: [],
        },
      ])
    } finally {
      setUploadState({ active: false, progress: 0, fileName: '' })
    }
  }

  const handleDeleteDocument = async (documentId) => {
    try {
      await deleteDocument(documentId)
      setDocuments((prev) => {
        const remaining = prev.filter((doc) => doc.id !== documentId)
        setSelectedDocumentId((current) =>
          current === documentId ? remaining[0]?.id || '' : current,
        )
        return remaining
      })
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: 'Unable to delete that document at the moment.',
          citations: [],
        },
      ])
    }
  }

  const handleSend = async (query) => {
    const payload = {
      query,
      documentId: selectedDocumentId || null,
      topK: 5,
      similarityThreshold: 0.0,
      conversationId:
        [...messages].reverse().find((item) => item.role === 'assistant')?.conversationId || null,
    }

    pushUserMessage(query)
    setPending(true)

    try {
      if (streamMode) {
        const streamMessageId = crypto.randomUUID()
        setMessages((prev) => [...prev, { id: streamMessageId, role: 'assistant', content: '', citations: [] }])
        await streamGenerateResponse(payload, (text) => {
          setMessages((prev) =>
            prev.map((item) => (item.id === streamMessageId ? { ...item, content: text } : item)),
          )
        })
        return
      }

      const response = await generateResponse(payload)
      const responseData = response?.data || {}
      pushAssistantMessage({
        content: responseData.response || 'No response generated.',
        citations: responseData.citations || [],
        conversationId: responseData.conversationId || null,
      })

      await searchSimilar(payload).catch(() => null)
    } catch (error) {
      pushAssistantMessage({
        content: error.message || 'I could not generate an answer. Please try again.',
        citations: [],
      })
    } finally {
      setPending(false)
    }
  }

  if (!isAuthenticated) {
    return <AuthModal />
  }

  return (
    <div className="relative min-h-screen bg-slate-950 text-slate-100">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_10%_20%,rgba(99,102,241,0.15),transparent_35%),radial-gradient(circle_at_80%_10%,rgba(139,92,246,0.1),transparent_35%)]" />
      <div className="relative flex min-h-screen">
        <aside className="hidden xl:block xl:w-80">
          <Sidebar
            mobileOpen={false}
            onCloseMobile={() => {}}
            documents={documents}
            selectedDocumentId={selectedDocumentId}
            onSelectDocument={setSelectedDocumentId}
            uploadState={uploadState}
            onUploadFiles={handleUploadFiles}
            onDeleteDocument={handleDeleteDocument}
            workspace={workspace}
            onWorkspaceChange={setWorkspace}
            user={user}
            onLogout={logout}
          />
        </aside>

        <AnimatePresence>
          {mobileSidebarOpen && (
            <Sidebar
              mobileOpen={mobileSidebarOpen}
              onCloseMobile={() => setMobileSidebarOpen(false)}
              documents={documents}
              selectedDocumentId={selectedDocumentId}
              onSelectDocument={(id) => {
                setSelectedDocumentId(id)
                setMobileSidebarOpen(false)
              }}
              uploadState={uploadState}
              onUploadFiles={handleUploadFiles}
              onDeleteDocument={handleDeleteDocument}
              workspace={workspace}
              onWorkspaceChange={setWorkspace}
              user={user}
              onLogout={logout}
            />
          )}
        </AnimatePresence>

        <motion.main layout className="flex flex-1">
          <ChatPanel
            selectedDocument={selectedDocument}
            messages={messages}
            pending={pending}
            streamMode={streamMode}
            onToggleStreamMode={() => setStreamMode((prev) => !prev)}
            onSend={handleSend}
            onClearHistory={() => setMessages([])}
            onOpenMobileSidebar={() => setMobileSidebarOpen(true)}
          />
        </motion.main>
      </div>
    </div>
  )
}

export default App
