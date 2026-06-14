<script setup lang="ts">
import { onMounted, onUpdated, ref, watch } from 'vue'
import { renderMarkdown, renderMarkdownSync } from '@/lib/markdown'

const props = defineProps<{ text: string }>()

const html = ref('')
const container = ref<HTMLElement | null>(null)

async function update() {
  // First paint immediately with whatever sync renderer we have, then upgrade.
  html.value = renderMarkdownSync(props.text)
  try {
    html.value = await renderMarkdown(props.text)
  } catch (e) {
    // Fallback to sync rendering; surface the error in console only.
    // eslint-disable-next-line no-console
    console.warn('markdown render failed', e)
  }
}

watch(() => props.text, update, { immediate: true })

function enhanceCodeBlocks() {
  const root = container.value
  if (!root) return
  const wrappers = root.querySelectorAll<HTMLElement>('.code-block-wrapper:not([data-enhanced])')
  wrappers.forEach((wrap) => {
    wrap.dataset.enhanced = 'true'
    const button = document.createElement('button')
    button.type = 'button'
    button.className =
      'code-copy-btn absolute right-2 top-2 rounded border border-border bg-background/90 px-2 py-1 text-xs text-muted-foreground transition hover:text-foreground'
    button.textContent = '复制'
    button.addEventListener('click', async () => {
      const raw = decodeURIComponent(wrap.dataset.raw ?? '')
      try {
        await navigator.clipboard.writeText(raw)
        const original = button.textContent
        button.textContent = '已复制'
        setTimeout(() => {
          button.textContent = original ?? '复制'
        }, 1200)
      } catch {
        button.textContent = '复制失败'
        setTimeout(() => {
          button.textContent = '复制'
        }, 1200)
      }
    })
    wrap.style.position = 'relative'
    wrap.appendChild(button)
  })
}

onMounted(enhanceCodeBlocks)
onUpdated(enhanceCodeBlocks)
</script>

<template>
  <div ref="container" class="prose-chat" v-html="html" />
</template>
