<script setup lang="ts">
import { type HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

defineProps<{ class?: HTMLAttributes['class'] }>()
</script>

<template>
  <div :class="cn('flex flex-col space-y-2 text-center sm:text-left', $attrs.class as string)">
    <slot />
  </div>
</template>
