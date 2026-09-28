import { motion } from 'framer-motion'
import {
  FileText,
  Loader2,
  LogOut,
  Sparkles,
  Trash2,
  UploadCloud,
  UserCircle2,
  X,
} from 'lucide-react'
import { useMemo, useState } from 'react'

const STATUS_STYLES = {
  PROCESSING: 'bg-amber-500/15 text-amber-300 border-amber-400/30',
  UPLOADING: 'bg-cyan-500/15 text-cyan-300 border-cyan-400/30',
  PROCESSED: 'bg-emerald-500/15 text-emerald-300 border-emerald-400/30',
  INDEXED: 'bg-emerald-500/15 text-emerald-300 border-emerald-400/30',
  FAILED: 'bg-rose-500/15 text-rose-300 border-rose-400/30',
}

function toReadableStatus(status) {
  if (status === 'INDEXED') return 'Indexed'
  if (status === 'PROCESSED') return 'Indexed'
  if (status === 'FAILED') return 'Error'
  if (status === 'UPLOADING') return 'Uploading'
  return 'Processing'
}

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '—'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let i = 0
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i += 1
  }
  return `${size.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

export default function Sidebar({
  mobileOpen,
  onCloseMobile,
  documents,
  selectedDocumentId,
  onSelectDocument,
  uploadState,
  onUploadFiles,
  onDeleteDocument,
  workspace,
  onWorkspaceChange,
  user,
  onLogout,
}) {
  const [dragActive, setDragActive] = useState(false)

  const selectedDoc = useMemo(
    () => documents.find((doc) => doc.id === selectedDocumentId) || null,
    [documents, selectedDocumentId],
  )

  const handleFiles = (fileList) => {
    const files = [...fileList].filter((file) => /pdf|msword|officedocument|text/i.test(file.type))
    if (files.length > 0) onUploadFiles(files)
  }

  const panel = (
    <div className="flex h-full w-full flex-col border-r border-slate-800/80 bg-slate-900/80 backdrop-blur xl:w-80">
      <div className="border-b border-slate-800/80 px-5 py-4">
        <div className="flex items-center gap-3">
          <div className="rounded-lg border border-cyan-400/40 bg-cyan-400/10 p-2 text-cyan-300 shadow-glow">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <p className="text-lg font-semibold text-slate-100">Paper-Pilot AI</p>
            <p className="text-xs text-slate-400">Document Workspace</p>
          </div>
          <button
            type="button"
            className="ml-auto rounded-md p-1 text-slate-400 hover:bg-slate-800 hover:text-slate-200 xl:hidden"
            onClick={onCloseMobile}
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>

      <div className="space-y-4 p-4">
        <label
          htmlFor="document-upload"
          onDragEnter={(e) => {
            e.preventDefault()
            setDragActive(true)
          }}
          onDragLeave={(e) => {
            e.preventDefault()
            setDragActive(false)
          }}
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault()
            setDragActive(false)
            handleFiles(e.dataTransfer.files)
          }}
          className={`block cursor-pointer rounded-xl border border-dashed p-4 text-center transition ${
            dragActive
              ? 'border-cyan-400 bg-cyan-500/10'
              : 'border-slate-700 bg-slate-950/60 hover:border-violet-500/60'
          }`}
        >
          <UploadCloud className="mx-auto mb-2 h-6 w-6 text-cyan-400" />
          <p className="text-sm font-medium text-slate-200">Drop PDFs or docs here</p>
          <p className="mt-1 text-xs text-slate-400">or click to browse and upload</p>
          <input
            id="document-upload"
            type="file"
            className="hidden"
            multiple
            accept=".pdf,.doc,.docx,.txt"
            onChange={(e) => {
              if (e.target.files) handleFiles(e.target.files)
            }}
          />
        </label>

        {uploadState.active && (
          <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
            <div className="mb-2 flex items-center justify-between text-xs text-slate-300">
              <span className="inline-flex items-center gap-1">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Uploading {uploadState.fileName}
              </span>
              <span>{uploadState.progress}%</span>
            </div>
            <div className="h-2 w-full rounded-full bg-slate-800">
              <div
                className="h-full rounded-full bg-gradient-to-r from-violet-600 to-indigo-500 transition-all"
                style={{ width: `${uploadState.progress}%` }}
              />
            </div>
          </div>
        )}
      </div>

      <div className="min-h-0 flex-1 px-4 pb-4">
        <div className="mb-2 flex items-center justify-between px-1">
          <h2 className="text-sm font-semibold text-slate-200">Uploaded Documents</h2>
          <span className="text-xs text-slate-400">{documents.length}</span>
        </div>
        <div className="h-full space-y-2 overflow-y-auto pr-1">
          {documents.map((doc) => (
            <div
              key={doc.id}
              onClick={() => onSelectDocument(doc.id)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault()
                  onSelectDocument(doc.id)
                }
              }}
              role="button"
              tabIndex={0}
              className={`w-full rounded-xl border p-3 text-left transition ${
                selectedDocumentId === doc.id
                  ? 'border-violet-500/60 bg-violet-500/10'
                  : 'border-slate-800 bg-slate-950/70 hover:border-slate-700'
              }`}
            >
              <div className="flex items-start gap-2">
                <FileText className="mt-0.5 h-4 w-4 shrink-0 text-cyan-400" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-slate-100">{doc.filename}</p>
                  <p className="mt-0.5 text-xs text-slate-400">{formatSize(doc.fileSize)}</p>
                </div>
                <button
                  type="button"
                  className="rounded-md p-1 text-slate-500 hover:bg-rose-500/20 hover:text-rose-300"
                  onClick={(event) => {
                    event.stopPropagation()
                    onDeleteDocument(doc.id)
                  }}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>

              <span
                className={`mt-3 inline-flex rounded-md border px-2 py-1 text-[10px] font-semibold uppercase tracking-wide ${
                  STATUS_STYLES[doc.status] || STATUS_STYLES.PROCESSING
                }`}
              >
                {toReadableStatus(doc.status)}
              </span>
            </div>
          ))}
          {documents.length === 0 && (
            <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4 text-center text-xs text-slate-500">
              No documents uploaded yet.
            </div>
          )}
        </div>
      </div>

      <div className="border-t border-slate-800/80 px-4 py-3">
        <div className="mb-3 flex items-center gap-2 rounded-lg border border-slate-800 bg-slate-950/60 px-3 py-2">
          <UserCircle2 className="h-4 w-4 text-cyan-400" />
          <select
            value={workspace}
            onChange={(e) => onWorkspaceChange(e.target.value)}
            className="w-full bg-transparent text-sm text-slate-200 outline-none"
          >
            <option value="research">Research Workspace</option>
            <option value="compliance">Compliance Workspace</option>
            <option value="team">Team Workspace</option>
          </select>
        </div>
        <div className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/60 px-3 py-2">
          <div>
            <p className="text-sm text-slate-200">{user?.fullName || 'Paper-Pilot User'}</p>
            <p className="text-xs text-slate-500">{user?.email}</p>
          </div>
          <button
            type="button"
            onClick={onLogout}
            className="inline-flex items-center gap-1 rounded-md border border-slate-700 px-2 py-1 text-xs text-slate-300 hover:border-slate-600 hover:bg-slate-800"
          >
            <LogOut className="h-3.5 w-3.5" />
            Logout
          </button>
        </div>
      </div>
    </div>
  )

  if (!mobileOpen) {
    return <aside className="hidden xl:block xl:w-80">{panel}</aside>
  }

  return (
    <motion.aside
      initial={{ x: -420, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      exit={{ x: -420, opacity: 0 }}
      className="fixed inset-0 z-50 xl:hidden"
    >
      <div className="absolute inset-0 bg-slate-950/70 backdrop-blur-sm" onClick={onCloseMobile} />
      <div className="relative h-full w-80 max-w-[92vw]">{panel}</div>
    </motion.aside>
  )
}

export function SelectedDocumentChip({ filename }) {
  if (!filename) {
    return <span className="rounded-md border border-slate-700 px-2 py-1 text-xs text-slate-400">No document selected</span>
  }
  return (
    <span className="rounded-md border border-cyan-500/40 bg-cyan-500/10 px-2 py-1 text-xs text-cyan-300">
      {filename}
    </span>
  )
}
