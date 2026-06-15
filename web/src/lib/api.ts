import type { AuthUser, ChatPayload } from './types'

const BASE = '/api'

interface ApiEnvelope<T> {
  code: number
  data: T
  message: string
}

export class HttpError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'HttpError'
  }
}

async function parseError(res: Response): Promise<string> {
  const contentType = res.headers.get('content-type') ?? ''
  try {
    if (contentType.includes('application/json')) {
      const body = (await res.json()) as Partial<ApiEnvelope<unknown>>
      if (typeof body.message === 'string' && body.message.trim()) {
        return body.message
      }
    } else {
      const text = await res.text()
      if (text.trim()) return text
    }
  } catch {
    // Ignore parser failures and fall back to generic text below.
  }
  return `request failed (${res.status})`
}

async function requestEnvelope<T>(path: string, init: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    credentials: 'include',
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers ?? {}),
    },
  })

  if (!res.ok) {
    throw new HttpError(res.status, await parseError(res))
  }

  const body = (await res.json()) as ApiEnvelope<T>
  return body.data
}

export function sendCode(email: string): Promise<{ ok: boolean }> {
  return requestEnvelope('/auth/sendCode', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function verifyCode(email: string, code: string): Promise<{ ok: boolean }> {
  return requestEnvelope('/auth/verifyCode', {
    method: 'POST',
    body: JSON.stringify({ email, code }),
  })
}

export function register(payload: {
  email: string
  code: string
  nickname: string
  password: string
}): Promise<AuthUser> {
  return requestEnvelope('/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function login(payload: { email: string; password: string }): Promise<AuthUser> {
  return requestEnvelope('/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function logout(): Promise<{ ok: boolean }> {
  return requestEnvelope('/auth/logout', {
    method: 'POST',
    body: JSON.stringify({}),
  })
}

export function me(): Promise<AuthUser> {
  return requestEnvelope('/auth/me', {
    method: 'GET',
  })
}

/** Synchronous one-shot chat. Returns the full reply text. */
export async function chat(payload: ChatPayload, signal?: AbortSignal): Promise<string> {
  const res = await fetch(`${BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(payload),
    signal,
  })
  if (!res.ok) {
    throw new HttpError(res.status, await parseError(res))
  }
  return await res.text()
}

/**
 * Streaming chat. Reads the response body chunk-by-chunk and forwards each
 * decoded UTF-8 string fragment to `onChunk`. The promise resolves when the
 * stream ends and rejects on network/HTTP errors or when `signal` aborts.
 */
export async function streamChat(
  payload: ChatPayload,
  signal: AbortSignal | undefined,
  onChunk: (chunk: string) => void,
): Promise<void> {
  const res = await fetch(`${BASE}/streamChat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(payload),
    signal,
  })
  if (!res.ok) {
    throw new HttpError(res.status, await parseError(res))
  }
  if (!res.body) {
    throw new Error('streamChat: response body missing')
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        const tail = decoder.decode()
        if (tail) onChunk(tail)
        break
      }
      const piece = decoder.decode(value, { stream: true })
      if (piece) onChunk(piece)
    }
  } finally {
    try {
      reader.releaseLock()
    } catch {
      // releaseLock can throw if reader is already closed; ignore.
    }
  }
}
