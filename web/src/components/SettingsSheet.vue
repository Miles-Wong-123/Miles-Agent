<script setup lang="ts">
import { storeToRefs } from 'pinia'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet'
import { Switch } from '@/components/ui/switch'
import { Button } from '@/components/ui/button'
import { useSettingsStore, type ThemeMode } from '@/stores/settings'
import { useChatStore } from '@/stores/chat'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'update:open', value: boolean): void }>()

const settings = useSettingsStore()
const { userId, sessionId, useStreaming, theme } = storeToRefs(settings)
const chat = useChatStore()

const themeOptions: ThemeMode[] = ['light', 'dark', 'system']

function resetSession() {
  chat.resetSession()
}
</script>

<template>
  <Sheet :open="props.open" @update:open="(v: boolean) => emit('update:open', v)">
    <SheetContent side="right" class="w-full sm:max-w-md">
      <SheetHeader>
        <SheetTitle>设置</SheetTitle>
        <SheetDescription>调整 userId、会话与外观偏好。修改会持久化到本地。</SheetDescription>
      </SheetHeader>

      <div class="mt-6 flex flex-col gap-6">
        <div class="flex flex-col gap-2">
          <label class="text-sm font-medium text-foreground" for="settings-userid">用户 ID</label>
          <input
            id="settings-userid"
            v-model="userId"
            type="text"
            class="h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
            placeholder="你的 ID（用于监控打点）"
          />
        </div>

        <div class="flex flex-col gap-2">
          <span class="text-sm font-medium text-foreground">会话 ID</span>
          <div class="flex items-center gap-2">
            <code class="flex-1 truncate rounded-md bg-muted px-2 py-1.5 text-xs text-muted-foreground">
              {{ sessionId }}
            </code>
            <Button type="button" variant="outline" size="sm" @click="resetSession">重置会话</Button>
          </div>
          <p class="text-xs text-muted-foreground">
            重置后会清空当前对话；后端 Redis 中旧 sessionId 的记忆保持不变（自然过期）。
          </p>
        </div>

        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-foreground">流式输出</p>
            <p class="text-xs text-muted-foreground">关闭后将走同步 /chat 接口。</p>
          </div>
          <Switch v-model:checked="useStreaming" />
        </div>

        <div class="flex flex-col gap-2">
          <span class="text-sm font-medium text-foreground">主题</span>
          <div class="grid grid-cols-3 gap-2">
            <Button
              v-for="mode in themeOptions"
              :key="mode"
              type="button"
              :variant="theme === mode ? 'default' : 'outline'"
              size="sm"
              @click="theme = mode"
            >
              {{ mode === 'light' ? '浅色' : mode === 'dark' ? '深色' : '跟随系统' }}
            </Button>
          </div>
        </div>
      </div>
    </SheetContent>
  </Sheet>
</template>
