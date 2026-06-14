<script setup lang="ts">
import { Moon, Sun } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useTheme } from '@/composables/useTheme'
import { useSettingsStore } from '@/stores/settings'

const { isDark } = useTheme()
const settings = useSettingsStore()

function toggle() {
  // Switch to explicit light/dark; this also takes you out of 'system' mode.
  settings.theme = isDark.value ? 'light' : 'dark'
}
</script>

<template>
  <Button
    type="button"
    variant="ghost"
    size="icon"
    :aria-label="isDark ? '切换浅色主题' : '切换深色主题'"
    @click="toggle"
  >
    <Moon v-if="isDark" class="h-4 w-4" />
    <Sun v-else class="h-4 w-4" />
  </Button>
</template>
