export type ChatRole = 'user' | 'ai'

export interface ChatPayload {
  sessionId: string
  userId: string
  prompt: string
}

export interface ChatErrorState {
  message: string
  /** Whether the user can retry by sending the same prompt again */
  retryable: boolean
}

export interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  /** True while AI message is actively streaming */
  streaming?: boolean
  /** Set when the user explicitly stopped this message mid-stream */
  stoppedByUser?: boolean
  /** Set when the message ended in an error (network, server, mid-stream disconnect) */
  error?: ChatErrorState
  createdAt: number
}
