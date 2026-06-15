import { defineStore } from 'pinia'
import { v4 as uuidv4 } from 'uuid'
import { HttpError, chat as apiChat, streamChat as apiStreamChat } from '@/lib/api'
import type { ChatMessage } from '@/lib/types'
import { useSettingsStore } from './settings'

interface ChatState {
  messages: ChatMessage[]
  isStreaming: boolean
}

function newMessage(role: ChatMessage['role'], content = ''): ChatMessage {
  return {
    id: uuidv4(),
    role,
    content,
    createdAt: Date.now(),
  }
}

function describeError(e: unknown): string {
  if (e instanceof HttpError) return `服务返回 ${e.status}`
  if (e instanceof TypeError) return '无法连接后端'
  if (e instanceof Error) return e.message
  return '未知错误'
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    messages: [],
    isStreaming: false,
  }),
  getters: {
    lastAiMessage: (state) =>
      [...state.messages].reverse().find((m) => m.role === 'ai') ?? null,
  },
  actions: {
    /** Send a brand-new user prompt and produce one AI reply. */
    async send(prompt: string) {
      const trimmed = prompt.trim()
      if (!trimmed) return
      if (this.isStreaming) return

      const user = newMessage('user', trimmed)
      const assistant = newMessage('ai', '')
      this.messages.push(user, assistant)
      // Re-read through the reactive state so target mutations actually propagate
      // to the UI; the local `assistant` variable is a non-reactive copy.
      const reactiveTarget = this.messages[this.messages.length - 1]
      await this.runAssistant(reactiveTarget, trimmed)
    },

    /**
     * Re-issue the prompt that produced the most recent AI message and
     * stream into the same message id, in place. No new user message added.
     */
    async regenerate() {
      if (this.isStreaming) return
      const lastAiIndex = [...this.messages].reverse().findIndex((m) => m.role === 'ai')
      if (lastAiIndex === -1) return
      const aiIndex = this.messages.length - 1 - lastAiIndex
      const ai = this.messages[aiIndex]
      const prevUser = this.messages
        .slice(0, aiIndex)
        .reverse()
        .find((m) => m.role === 'user')
      if (!prevUser) return
      ai.content = ''
      ai.error = undefined
      ai.stoppedByUser = false
      await this.runAssistant(ai, prevUser.content)
    },

    /** Stop the current stream. Already-received content is preserved. */
    stop() {
      const ctrl = (this as unknown as { _abortController?: AbortController })._abortController
      if (ctrl) {
        ctrl.abort()
      }
    },

    /** Wipe local conversation state and rotate sessionId. */
    resetSession() {
      this.stop()
      this.messages = []
      const settings = useSettingsStore()
      settings.resetSession()
    },

    async runAssistant(target: ChatMessage, prompt: string) {
      const settings = useSettingsStore()
      const payload = {
        sessionId: settings.sessionId,
        prompt,
      }

      const ctrl = new AbortController()
      ;(this as unknown as { _abortController?: AbortController })._abortController = ctrl
      this.isStreaming = true
      target.streaming = true
      target.error = undefined
      target.stoppedByUser = false

      try {
        if (settings.useStreaming) {
          await apiStreamChat(payload, ctrl.signal, (chunk) => {
            target.content += chunk
          })
        } else {
          target.content = await apiChat(payload, ctrl.signal)
        }
      } catch (e) {
        if (ctrl.signal.aborted) {
          target.stoppedByUser = true
        } else {
          target.error = { message: describeError(e), retryable: true }
        }
      } finally {
        target.streaming = false
        this.isStreaming = false
        ;(this as unknown as { _abortController?: AbortController })._abortController = undefined
      }
    },
  },
})
