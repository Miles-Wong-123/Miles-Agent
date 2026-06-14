import { beforeEach, describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ChatInput from '@/components/ChatInput.vue'

beforeEach(() => {
  setActivePinia(createPinia())
})

function makeWrapper(isStreaming = false) {
  return mount(ChatInput, {
    props: { isStreaming },
  })
}

describe('<ChatInput>', () => {
  it('disables send when prompt is empty', () => {
    const w = makeWrapper(false)
    const sendBtn = w.get('button[aria-label="发送"]')
    expect(sendBtn.attributes('disabled')).toBeDefined()
  })

  it('emits send on Enter and clears the textarea', async () => {
    const w = makeWrapper(false)
    const ta = w.get('textarea')
    await ta.setValue('hello')
    await flushPromises()
    await ta.trigger('keydown', { key: 'Enter' })

    expect(w.emitted('send')).toBeTruthy()
    expect(w.emitted('send')![0]).toEqual(['hello'])
    expect((ta.element as HTMLTextAreaElement).value).toBe('')
  })

  it('inserts newline on Shift+Enter and does not emit send', async () => {
    const w = makeWrapper(false)
    const ta = w.get('textarea')
    await ta.setValue('line1')
    await ta.trigger('keydown', { key: 'Enter', shiftKey: true })
    expect(w.emitted('send')).toBeFalsy()
  })

  it('does not emit send for whitespace-only prompts', async () => {
    const w = makeWrapper(false)
    const ta = w.get('textarea')
    await ta.setValue('   ')
    await ta.trigger('keydown', { key: 'Enter' })
    expect(w.emitted('send')).toBeFalsy()
  })

  it('warns and disables send when over the 8000-char limit', async () => {
    const w = makeWrapper(false)
    const ta = w.get('textarea')
    const big = 'a'.repeat(8001)
    await ta.setValue(big)
    await flushPromises()

    expect(w.text()).toContain('8000')
    const sendBtn = w.get('button[aria-label="发送"]')
    expect(sendBtn.attributes('disabled')).toBeDefined()
  })

  it('shows the stop button while streaming', async () => {
    const w = makeWrapper(true)
    expect(w.find('button[aria-label="停止生成"]').exists()).toBe(true)
    expect(w.find('button[aria-label="发送"]').exists()).toBe(false)
  })
})
