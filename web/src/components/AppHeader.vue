<script setup lang="ts">
import { Settings } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import type { AuthUser } from '@/lib/types'
import ThemeToggle from './ThemeToggle.vue'

defineProps<{
  user: AuthUser | null
}>()

defineEmits<{
  (e: 'open-settings'): void
  (e: 'logout'): void
}>()
</script>

<template>
  <header class="flex h-14 items-center justify-between border-b border-border bg-background/95 px-4 backdrop-blur">
    <div class="flex items-center gap-2">
      <span class="text-sm font-semibold tracking-tight text-foreground">Miles-Agent</span>
      <span class="text-xs text-muted-foreground">·  AI 对话</span>
    </div>
    <div class="flex items-center gap-2">
      <div v-if="user" class="flex items-center gap-2">
        <span class="hidden text-sm text-muted-foreground sm:inline">{{ user.nickname }}</span>
        <Button type="button" variant="outline" size="sm" @click="$emit('logout')">退出登录</Button>
      </div>
      <ThemeToggle />
      <Button
        type="button"
        variant="ghost"
        size="icon"
        aria-label="设置"
        @click="$emit('open-settings')"
      >
        <Settings class="h-4 w-4" />
      </Button>
    </div>
  </header>
</template>
