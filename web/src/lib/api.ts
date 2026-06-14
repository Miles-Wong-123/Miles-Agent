import type { ChatPayload } from './types'

const BASE = '/api'

export class HttpError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'HttpError'
  }
}

/** Synchronous one-shot chat. Returns the full reply text. */
export async function chat(payload: ChatPayload, signal?: AbortSignal): Promise<string> {
  const res = await fetch(`${BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    signal,
  })
  if (!res.ok) {
    throw new HttpError(res.status, `chat failed (${res.status})`)
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
    body: JSON.stringify(payload),
    signal,
  })
  if (!res.ok) {
    throw new HttpError(res.status, `streamChat failed (${res.status})`)
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
