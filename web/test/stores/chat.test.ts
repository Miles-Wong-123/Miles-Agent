import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useChatStore } from '@/stores/chat'
import * as api from '@/lib/api'

beforeEach(() => {
  setActivePinia(createPinia())
  // Stub localStorage so the persistedstate plugin doesn't interfere
  vi.stubGlobal('localStorage', {
    getItem: () => null,
    setItem: () => {},
    removeItem: () => {},
    clear: () => {},
    key: () => null,
    length: 0,
  })
})

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('chat store: send (streaming success)', () => {
  it('appends a user message + AI message and streams content into AI message', async () => {
    const streamSpy = vi
      .spyOn(api, 'streamChat')
      .mockImplementation(async (_payload, _signal, onChunk) => {
        onChunk('Hi ')
        onChunk('there')
      })

    const store = useChatStore()
    await store.send('Hello?')

    expect(store.messages).toHaveLength(2)
    expect(store.messages[0].role).toBe('user')
    expect(store.messages[0].content).toBe('Hello?')
    expect(store.messages[1].role).toBe('ai')
    expect(store.messages[1].content).toBe('Hi there')
    expect(store.isStreaming).toBe(false)
    expect(streamSpy).toHaveBeenCalledOnce()
  })

  it('records error on AI message when stream rejects', async () => {
    vi.spyOn(api, 'streamChat').mockRejectedValueOnce(new TypeError('Failed to fetch'))

    const store = useChatStore()
    await store.send('hi')

    const ai = store.messages.at(-1)!
    expect(ai.role).toBe('ai')
    expect(ai.error).toBeDefined()
    expect(ai.error?.message).toBe('无法连接后端')
    expect(ai.error?.retryable).toBe(true)
    expect(store.isStreaming).toBe(false)
  })

  it('marks AI message stoppedByUser when aborted', async () => {
    vi.spyOn(api, 'streamChat').mockImplementation(async (_p, signal) => {
      // Simulate abort: signal is aborted before fetch resolves.
      throw Object.assign(new DOMException('aborted', 'AbortError'), { name: 'AbortError' })
    })

    const store = useChatStore()
    // Pre-arm abort by stopping immediately after send starts.
    const sendPromise = store.send('hi')
    store.stop()
    await sendPromise

    const ai = store.messages.at(-1)!
    expect(ai.role).toBe('ai')
    expect(ai.stoppedByUser).toBe(true)
    expect(ai.error).toBeUndefined()
  })

  it('ignores send while already streaming', async () => {
    let resolveStream: (() => void) | undefined
    vi.spyOn(api, 'streamChat').mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveStream = resolve
        }),
    )

    const store = useChatStore()
    const first = store.send('one')
    expect(store.isStreaming).toBe(true)
    await store.send('two') // should be a no-op
    expect(store.messages).toHaveLength(2) // still just first user + first ai

    resolveStream?.()
    await first
  })

  it('skips empty / whitespace prompts', async () => {
    const spy = vi.spyOn(api, 'streamChat')
    const store = useChatStore()
    await store.send('   ')
    expect(spy).not.toHaveBeenCalled()
    expect(store.messages).toHaveLength(0)
  })
})

describe('chat store: regenerate', () => {
  it('is a no-op when there is no prior user message', async () => {
    const spy = vi.spyOn(api, 'streamChat')
    const store = useChatStore()
    await store.regenerate()
    expect(spy).not.toHaveBeenCalled()
    expect(store.messages).toHaveLength(0)
  })

  it('clears the last AI message and re-streams without adding a new user message', async () => {
    const streamSpy = vi
      .spyOn(api, 'streamChat')
      .mockImplementationOnce(async (_p, _s, onChunk) => onChunk('first'))
      .mockImplementationOnce(async (_p, _s, onChunk) => onChunk('second'))

    const store = useChatStore()
    await store.send('q')
    expect(store.messages.at(-1)!.content).toBe('first')
    expect(store.messages).toHaveLength(2)

    await store.regenerate()

    expect(store.messages).toHaveLength(2)
    expect(store.messages.at(-1)!.content).toBe('second')
    expect(streamSpy).toHaveBeenCalledTimes(2)
  })
})

describe('chat store: resetSession', () => {
  it('clears messages and rotates the sessionId', async () => {
    const streamSpy = vi
      .spyOn(api, 'streamChat')
      .mockImplementation(async (_p, _s, onChunk) => onChunk('x'))

    const store = useChatStore()
    await store.send('foo')
    expect(store.messages).toHaveLength(2)

    const settingsBefore = (await import('@/stores/settings')).useSettingsStore()
    const oldSessionId = settingsBefore.sessionId

    store.resetSession()

    expect(store.messages).toHaveLength(0)
    expect(settingsBefore.sessionId).not.toBe(oldSessionId)
    expect(streamSpy).toHaveBeenCalledOnce()
  })
})
