const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const AUTH_LOGIN_ENDPOINT = import.meta.env.VITE_AUTH_LOGIN_ENDPOINT || ''
const AUTH_REGISTER_ENDPOINT = import.meta.env.VITE_AUTH_REGISTER_ENDPOINT || ''

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...options.headers,
    },
    ...options,
  })

  const payload = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(payload?.message || 'Request failed')
  }
  return payload
}

export async function login({ email, password }) {
  if (!AUTH_LOGIN_ENDPOINT) {
    const mockToken = `mock-jwt-${Date.now()}`
    return {
      token: mockToken,
      user: {
        email,
        fullName: email.split('@')[0],
      },
    }
  }
  return request(AUTH_LOGIN_ENDPOINT, {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export async function register({ fullName, email, password }) {
  if (!AUTH_REGISTER_ENDPOINT) {
    const mockToken = `mock-jwt-${Date.now()}`
    return {
      token: mockToken,
      user: {
        email,
        fullName,
      },
    }
  }
  return request(AUTH_REGISTER_ENDPOINT, {
    method: 'POST',
    body: JSON.stringify({ fullName, email, password }),
  })
}

export async function getAllDocuments() {
  return request('/api/documents/all', { method: 'GET' })
}

export async function getDocumentById(documentId) {
  return request(`/api/documents/fetch/${documentId}`, { method: 'GET' })
}

export async function deleteDocument(documentId) {
  return request(`/api/documents/delete/${documentId}`, { method: 'DELETE' })
}

export function uploadDocument(file, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    const formData = new FormData()
    formData.append('document', file)

    xhr.open('POST', `${API_BASE_URL}/api/documents/upload`)
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        const progress = Math.round((event.loaded / event.total) * 100)
        onProgress(progress)
      }
    }

    xhr.onload = () => {
      try {
        const parsed = JSON.parse(xhr.responseText || '{}')
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve(parsed)
          return
        }
        reject(new Error(parsed?.message || 'Upload failed'))
      } catch {
        reject(new Error('Upload failed'))
      }
    }
    xhr.onerror = () => reject(new Error('Upload failed'))
    xhr.send(formData)
  })
}

export async function uploadMultipleDocuments(files) {
  const formData = new FormData()
  files.forEach((file) => formData.append('documents', file))
  return request('/api/documents/upload/multiple', {
    method: 'POST',
    body: formData,
  })
}

export async function generateResponse(payload) {
  return request('/api/chat/generate-response', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function searchSimilar(payload) {
  return request('/api/chat/search-similar', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function streamGenerateResponse(payload, onChunk) {
  const response = await fetch(`${API_BASE_URL}/api/chat/stream-generate-response`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

  if (!response.ok || !response.body) {
    throw new Error('Unable to stream response')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let fullText = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    fullText += chunk
    if (onChunk) {
      onChunk(fullText)
    }
  }

  return fullText
}
