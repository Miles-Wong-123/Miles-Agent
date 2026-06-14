<script setup lang="ts">
import { DialogTitle, type DialogTitleProps } from 'reka-ui'
import { type HTMLAttributes, computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<DialogTitleProps & { class?: HTMLAttributes['class'] }>()

const delegated = computed(() => {
  const { class: _c, ...rest } = props
  return rest
})
</script>

<template>
  <DialogTitle
    v-bind="delegated"
    :class="cn('text-lg font-semibold text-foreground', props.class)"
  >
    <slot />
  </DialogTitle>
</template>
