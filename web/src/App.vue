<script setup lang="ts">
import { RouterView } from 'vue-router'
import { Sparkles } from 'lucide-vue-next'
import { Sonner } from '@/components/ui/sonner'

// Sparkles is imported here as a smoke test for lucide-vue-next; the actual
// usage lives inside ChatMessage's AI avatar.
const _smokeIcon = Sparkles
void _smokeIcon
</script>

<template>
  <RouterView />
  <Sonner />
</template>
