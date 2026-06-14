import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ChatMessage from '@/components/ChatMessage.vue'
import type { ChatMessage as ChatMessageType } from '@/lib/types'

beforeEach(() => {
  setActivePinia(createPinia())
})

function baseMessage(overrides: Partial<ChatMessageType> = {}): ChatMessageType {
  return {
    id: 'm-1',
    role: 'ai',
    content: 'Hello',
    createdAt: 0,
    ...overrides,
  }
}

describe('<ChatMessage>', () => {
  it('renders user role as a muted card with raw text', () => {
    const w = mount(ChatMessage, {
      props: {
        message: baseMessage({ role: 'user', content: 'who are you?' }),
        isLastAi: false,
      },
    })
    expect(w.html()).toContain('bg-muted')
    expect(w.text()).toContain('who are you?')
    // user messages don't have hover actions for copy/regenerate
    expect(w.find('button[aria-label="重新生成"]').exists()).toBe(false)
  })

  it('renders ai role with MarkdownView (prose-chat container)', () => {
    const w = mount(ChatMessage, {
      props: {
        message: baseMessage({ role: 'ai', content: 'I am an AI.' }),
        isLastAi: true,
      },
    })
    expect(w.find('.prose-chat').exists()).toBe(true)
    expect(w.html()).toContain('lucide-sparkles')
  })

  it('shows muted "已停止" when stoppedByUser is true', () => {
    const w = mount(ChatMessage, {
      props: {
        message: baseMessage({ role: 'ai', stoppedByUser: true }),
        isLastAi: true,
      },
    })
    expect(w.text()).toContain('已停止')
  })

  it('shows error block + retry button on error state', async () => {
    const w = mount(ChatMessage, {
      props: {
        message: baseMessage({
          role: 'ai',
          content: '',
          error: { message: '无法连接后端', retryable: true },
        }),
        isLastAi: true,
      },
    })
    expect(w.text()).toContain('无法连接后端')
    const retryBtn = w.get('button')
    expect(retryBtn.text()).toContain('重试')
    await retryBtn.trigger('click')
    expect(w.emitted('retry')).toBeTruthy()
  })

  it('shows regenerate only for the last AI message', () => {
    const lastAi = mount(ChatMessage, {
      props: { message: baseMessage({ role: 'ai' }), isLastAi: true },
    })
    expect(lastAi.find('button[aria-label="重新生成"]').exists()).toBe(true)

    const earlierAi = mount(ChatMessage, {
      props: { message: baseMessage({ role: 'ai', id: 'm-2' }), isLastAi: false },
    })
    expect(earlierAi.find('button[aria-label="重新生成"]').exists()).toBe(false)
  })
})
