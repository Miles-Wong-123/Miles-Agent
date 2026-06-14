<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowDown } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { useChatStore } from '@/stores/chat'
import { useAutoScroll } from '@/composables/useAutoScroll'
import ChatMessage from './ChatMessage.vue'
import EmptyState from './EmptyState.vue'

const emit = defineEmits<{
  (e: 'send', prompt: string): void
  (e: 'regenerate'): void
  (e: 'retry'): void
}>()

const chat = useChatStore()
const { messages } = storeToRefs(chat)

const scrollContainer = ref<HTMLElement | null>(null)
const { showJumpToBottom, scrollToBottom, notify } = useAutoScroll(scrollContainer)

const isEmpty = computed(() => messages.value.length === 0)
const lastAiId = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'ai') return messages.value[i].id
  }
  return null
})

defineExpose({ notify })
</script>

<template>
  <div class="relative flex min-h-0 flex-1 flex-col">
    <div
      ref="scrollContainer"
      class="flex-1 overflow-y-auto scroll-smooth"
    >
      <div class="mx-auto flex min-h-full w-full max-w-chat flex-col px-6 py-8">
        <EmptyState v-if="isEmpty" @pick="emit('send', $event)" />
        <div v-else class="flex flex-col gap-6">
          <ChatMessage
            v-for="m in messages"
            :key="m.id"
            :message="m"
            :is-last-ai="m.id === lastAiId"
            @regenerate="emit('regenerate')"
            @retry="emit('retry')"
          />
        </div>
      </div>
    </div>

    <button
      v-if="showJumpToBottom"
      type="button"
      class="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-full border border-border bg-background/95 px-3 py-1.5 text-xs text-muted-foreground shadow-sm transition hover:text-foreground"
      @click="scrollToBottom"
    >
      <ArrowDown class="mr-1 inline h-3 w-3" /> 回到底部
    </button>
  </div>
</template>
