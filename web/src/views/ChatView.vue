<script setup lang="ts">
import { ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useChatStore } from '@/stores/chat'
import AppHeader from '@/components/AppHeader.vue'
import ChatList from '@/components/ChatList.vue'
import ChatInput from '@/components/ChatInput.vue'
import SettingsSheet from '@/components/SettingsSheet.vue'

const chat = useChatStore()
const { isStreaming, messages } = storeToRefs(chat)

const settingsOpen = ref(false)
const list = ref<InstanceType<typeof ChatList> | null>(null)

watch(
  () => messages.value.map((m) => m.content).join('|'),
  () => {
    list.value?.notify?.()
  },
)

function send(prompt: string) {
  void chat.send(prompt)
}

function regenerate() {
  void chat.regenerate()
}

function retry() {
  // Find the most recent user prompt and resend it (drops the last AI message).
  const ai = chat.lastAiMessage
  if (!ai) return
  const idx = messages.value.findIndex((m) => m.id === ai.id)
  if (idx <= 0) return
  const prevUser = [...messages.value.slice(0, idx)].reverse().find((m) => m.role === 'user')
  if (!prevUser) return
  ai.content = ''
  ai.error = undefined
  ai.stoppedByUser = false
  void chat.runAssistant(ai, prevUser.content)
}
</script>

<template>
  <div class="flex h-full min-h-screen flex-col bg-background text-foreground">
    <AppHeader @open-settings="settingsOpen = true" />
    <ChatList ref="list" @send="send" @regenerate="regenerate" @retry="retry" />
    <ChatInput :is-streaming="isStreaming" @send="send" @stop="chat.stop()" />
    <SettingsSheet v-model:open="settingsOpen" />
  </div>
</template>
