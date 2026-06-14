<script setup lang="ts">
import { Settings } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import ThemeToggle from './ThemeToggle.vue'

defineEmits<{
  (e: 'open-settings'): void
}>()
</script>

<template>
  <header class="flex h-14 items-center justify-between border-b border-border bg-background/95 px-4 backdrop-blur">
    <div class="flex items-center gap-2">
      <span class="text-sm font-semibold tracking-tight text-foreground">Miles-Agent</span>
      <span class="text-xs text-muted-foreground">·  AI 对话</span>
    </div>
    <div class="flex items-center gap-1">
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
