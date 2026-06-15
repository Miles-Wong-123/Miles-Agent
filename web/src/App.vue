<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { Sparkles } from 'lucide-vue-next'
import { Sonner } from '@/components/ui/sonner'
import { useAuthStore } from '@/stores/auth'

// Sparkles is imported here as a smoke test for lucide-vue-next; the actual
// usage lives inside ChatMessage's AI avatar.
const _smokeIcon = Sparkles
void _smokeIcon

const auth = useAuthStore()

onMounted(() => {
  void auth.ensureReady()
})
</script>

<template>
  <RouterView />
  <Sonner />
</template>
