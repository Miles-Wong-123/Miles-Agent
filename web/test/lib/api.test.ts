import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { HttpError, chat, streamChat } from '@/lib/api'

const fetchSpy = vi.fn()

beforeEach(() => {
  fetchSpy.mockReset()
  vi.stubGlobal('fetch', fetchSpy)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('chat()', () => {
  it('POSTs the payload as JSON and returns the response text', async () => {
    fetchSpy.mockResolvedValueOnce(new Response('hello'))

    const result = await chat({ sessionId: 's1', userId: 'u1', prompt: 'ping' })

    expect(result).toBe('hello')
    expect(fetchSpy).toHaveBeenCalledTimes(1)
    const [url, init] = fetchSpy.mock.calls[0]
    expect(url).toBe('/api/chat')
    expect(init.method).toBe('POST')
    expect(init.headers).toMatchObject({ 'Content-Type': 'application/json' })
    expect(JSON.parse(init.body as string)).toEqual({
      sessionId: 's1',
      userId: 'u1',
      prompt: 'ping',
    })
  })

  it('throws HttpError on non-ok responses', async () => {
    fetchSpy.mockResolvedValueOnce(new Response('boom', { status: 500 }))
    await expect(chat({ sessionId: 's', userId: 'u', prompt: 'p' })).rejects.toBeInstanceOf(
      HttpError,
    )
  })
})

describe('streamChat()', () => {
  function streamFromChunks(chunks: string[]): ReadableStream<Uint8Array> {
    const encoder = new TextEncoder()
    let i = 0
    return new ReadableStream({
      pull(controller) {
        if (i < chunks.length) {
          controller.enqueue(encoder.encode(chunks[i++]))
        } else {
          controller.close()
        }
      },
    })
  }

  it('forwards each decoded chunk to onChunk', async () => {
    const body = streamFromChunks(['Hello', ', ', 'world'])
    fetchSpy.mockResolvedValueOnce(new Response(body, { headers: { 'Content-Type': 'text/plain' } }))

    const received: string[] = []
    await streamChat({ sessionId: 's', userId: 'u', prompt: 'p' }, undefined, (c) => received.push(c))

    expect(received.join('')).toBe('Hello, world')
    expect(received.length).toBeGreaterThanOrEqual(1)
  })

  it('targets /api/streamChat with JSON body', async () => {
    fetchSpy.mockResolvedValueOnce(new Response(streamFromChunks(['a'])))
    await streamChat({ sessionId: 's', userId: 'u', prompt: 'p' }, undefined, () => {})

    const [url, init] = fetchSpy.mock.calls[0]
    expect(url).toBe('/api/streamChat')
    expect(init.method).toBe('POST')
    expect(JSON.parse(init.body as string)).toEqual({ sessionId: 's', userId: 'u', prompt: 'p' })
  })

  it('throws HttpError when response is not ok', async () => {
    fetchSpy.mockResolvedValueOnce(new Response('nope', { status: 502 }))
    await expect(
      streamChat({ sessionId: 's', userId: 'u', prompt: 'p' }, undefined, () => {}),
    ).rejects.toBeInstanceOf(HttpError)
  })
})
