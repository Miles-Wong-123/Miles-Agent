<script setup lang="ts">
const samples = [
  '帮我用 SQL 查找昨天活跃用户最多的 5 个城市',
  '我想做一份「敏捷开发与传统瀑布」对比，帮我给出 5 条对比维度',
  '写一段 Java 代码：把字符串里的所有重复字符去掉，保留第一次出现',
]

const emit = defineEmits<{
  (e: 'pick', prompt: string): void
}>()
</script>

<template>
  <div class="flex flex-1 flex-col items-center justify-center px-6 text-center">
    <h2 class="text-2xl font-semibold tracking-tight text-foreground">想聊点什么？</h2>
    <p class="mt-2 max-w-md text-sm text-muted-foreground">
      Miles-Agent 在线，有问题就丢过来。或者从下面的示例里挑一条。
    </p>
    <div class="mt-8 grid w-full max-w-xl gap-3 sm:grid-cols-1">
      <button
        v-for="sample in samples"
        :key="sample"
        type="button"
        class="rounded-xl border border-border bg-card px-4 py-3 text-left text-sm text-foreground transition hover:border-accent/50 hover:bg-accent/5"
        @click="emit('pick', sample)"
      >
        {{ sample }}
      </button>
    </div>
  </div>
</template>
