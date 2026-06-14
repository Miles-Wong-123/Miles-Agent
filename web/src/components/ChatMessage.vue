<script setup lang="ts">
import { Sparkles, RefreshCw, Copy, Check, AlertCircle } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import type { ChatMessage } from '@/lib/types'
import { Button } from '@/components/ui/button'
import MarkdownView from './MarkdownView.vue'

const props = defineProps<{
  message: ChatMessage
  isLastAi?: boolean
}>()

const emit = defineEmits<{
  (e: 'regenerate'): void
  (e: 'retry'): void
}>()

const copied = ref(false)

function plainText(): string {
  return props.message.content
    .replace(/```[\s\S]*?```/g, (block) => block.replace(/```/g, '').trim())
    .replace(/`([^`]+)`/g, '$1')
    .replace(/[*_~]+/g, '')
    .replace(/!\[[^\]]*\]\([^)]+\)/g, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .trim()
}

async function copyMessage() {
  try {
    await navigator.clipboard.writeText(plainText())
    copied.value = true
    setTimeout(() => (copied.value = false), 1200)
  } catch {
    copied.value = false
  }
}

const isUser = computed(() => props.message.role === 'user')
const showStopped = computed(() => props.message.stoppedByUser)
const errorState = computed(() => props.message.error)
</script>

<template>
  <div class="message group/message" :data-role="message.role">
    <template v-if="isUser">
      <div class="rounded-2xl bg-muted px-5 py-3 text-foreground">
        <div class="whitespace-pre-wrap break-words text-[15px] leading-7">
          {{ message.content }}
        </div>
      </div>
    </template>

    <template v-else>
      <div class="flex gap-3">
        <Sparkles
          class="mt-1 h-4 w-4 shrink-0 text-accent"
          aria-hidden="true"
        />
        <div class="min-w-0 flex-1">
          <MarkdownView v-if="message.content" :text="message.content" />
          <span
            v-else-if="message.streaming"
            class="inline-block h-4 w-2 animate-pulse rounded-sm bg-muted-foreground/40"
            aria-label="正在生成"
          />

          <span v-if="showStopped" class="ml-1 text-sm text-muted-foreground">已停止</span>

          <div
            v-if="errorState"
            class="mt-2 flex items-center gap-2 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive"
          >
            <AlertCircle class="h-4 w-4" aria-hidden="true" />
            <span class="flex-1">{{ errorState.message }}</span>
            <Button v-if="errorState.retryable" variant="outline" size="sm" @click="emit('retry')">
              重试
            </Button>
          </div>

          <div
            v-if="!message.streaming"
            class="mt-2 flex items-center gap-1 opacity-0 transition-opacity group-hover/message:opacity-100"
          >
            <Button variant="ghost" size="sm" :aria-label="copied ? '已复制' : '复制'" @click="copyMessage">
              <Check v-if="copied" class="h-3.5 w-3.5" />
              <Copy v-else class="h-3.5 w-3.5" />
              <span class="text-xs">{{ copied ? '已复制' : '复制' }}</span>
            </Button>
            <Button
              v-if="isLastAi"
              variant="ghost"
              size="sm"
              aria-label="重新生成"
              @click="emit('regenerate')"
            >
              <RefreshCw class="h-3.5 w-3.5" />
              <span class="text-xs">重新生成</span>
            </Button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.message {
  width: 100%;
}
</style>
