<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ArrowUp, Square } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'

const MAX_LEN = 8000
const MAX_ROWS = 10
const LINE_HEIGHT = 24

const props = defineProps<{
  isStreaming: boolean
}>()

const emit = defineEmits<{
  (e: 'send', prompt: string): void
  (e: 'stop'): void
}>()

const text = ref('')
const textarea = ref<HTMLTextAreaElement | null>(null)

const charCount = computed(() => text.value.length)
const tooLong = computed(() => charCount.value > MAX_LEN)
const trimmed = computed(() => text.value.trim())
const canSend = computed(() => !props.isStreaming && trimmed.value.length > 0 && !tooLong.value)

async function autosize() {
  const el = textarea.value
  if (!el) return
  el.style.height = 'auto'
  await nextTick()
  const max = LINE_HEIGHT * MAX_ROWS
  el.style.height = `${Math.min(el.scrollHeight, max)}px`
}

watch(text, autosize, { flush: 'post' })

function send() {
  if (!canSend.value) return
  const value = trimmed.value
  text.value = ''
  emit('send', value)
  void autosize()
}

function onKey(e: KeyboardEvent) {
  if (e.key !== 'Enter') return
  // Cmd/Ctrl+Enter sends; Shift+Enter newlines; bare Enter sends.
  if (e.shiftKey) return
  e.preventDefault()
  if (props.isStreaming) {
    emit('stop')
    return
  }
  send()
}
</script>

<template>
  <div class="border-t border-border bg-background/95 backdrop-blur">
    <div class="mx-auto w-full max-w-chat px-4 py-4">
      <div class="relative rounded-2xl border border-input bg-card shadow-sm focus-within:border-accent/60">
        <textarea
          ref="textarea"
          v-model="text"
          rows="1"
          placeholder="输入你的问题，回车发送，Shift+回车换行"
          class="block w-full resize-none border-0 bg-transparent px-4 py-3 pr-14 text-[15px] leading-6 text-foreground placeholder:text-muted-foreground focus:outline-none"
          :style="{ maxHeight: `${LINE_HEIGHT * MAX_ROWS}px` }"
          :maxlength="MAX_LEN + 200"
          @keydown="onKey"
        />
        <div class="absolute bottom-2 right-2 flex items-center gap-2">
          <span
            v-if="charCount > MAX_LEN - 1000"
            :class="['text-xs', tooLong ? 'text-destructive' : 'text-muted-foreground']"
          >
            {{ charCount }}/{{ MAX_LEN }}
          </span>
          <Button
            v-if="isStreaming"
            type="button"
            variant="accent"
            size="icon"
            class="h-8 w-8 rounded-full"
            aria-label="停止生成"
            @click="emit('stop')"
          >
            <Square class="h-3.5 w-3.5" />
          </Button>
          <Button
            v-else
            type="button"
            variant="accent"
            size="icon"
            class="h-8 w-8 rounded-full"
            :disabled="!canSend"
            aria-label="发送"
            @click="send"
          >
            <ArrowUp class="h-4 w-4" />
          </Button>
        </div>
      </div>
      <p v-if="tooLong" class="mt-1 px-2 text-xs text-destructive">
        消息超过 {{ MAX_LEN }} 字符上限，请精简后再发送。
      </p>
    </div>
  </div>
</template>
