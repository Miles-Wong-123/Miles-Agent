<script setup lang="ts">
import { TooltipProvider, type TooltipProviderProps } from 'reka-ui'

withDefaults(defineProps<TooltipProviderProps>(), { delayDuration: 200 })
</script>

<template>
  <TooltipProvider :delay-duration="delayDuration">
    <slot />
  </TooltipProvider>
</template>
