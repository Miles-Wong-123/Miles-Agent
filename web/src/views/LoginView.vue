<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { HttpError } from '@/lib/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const loading = ref(false)

const canSubmit = computed(() => email.value.trim().length > 0 && password.value.length > 0 && !loading.value)

async function submit() {
  if (!canSubmit.value) return
  loading.value = true
  try {
    await auth.login({
      email: email.value.trim(),
      password: password.value,
    })
    await router.push({ name: 'chat' })
  } catch (error) {
    const message = error instanceof HttpError ? error.message : '登录失败，请稍后重试'
    toast.error('登录失败', { description: message })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
    <div class="w-full max-w-md rounded-3xl border border-border bg-card/95 p-8 shadow-sm">
      <div class="space-y-2">
        <p class="text-sm text-muted-foreground">Miles-Agent</p>
        <h1 class="text-3xl font-semibold tracking-tight">邮箱登录</h1>
        <p class="text-sm leading-6 text-muted-foreground">
          登录后才能继续使用聊天、流式回复和知识写入能力。
        </p>
      </div>

      <form class="mt-8 space-y-4" @submit.prevent="submit">
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground" for="login-email">邮箱</label>
          <input
            id="login-email"
            v-model="email"
            type="email"
            autocomplete="email"
            class="h-11 w-full rounded-xl border border-input bg-background px-4 text-sm text-foreground outline-none transition focus:border-accent/50 focus:ring-2 focus:ring-accent/20"
            placeholder="name@example.com"
          />
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground" for="login-password">密码</label>
          <input
            id="login-password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            class="h-11 w-full rounded-xl border border-input bg-background px-4 text-sm text-foreground outline-none transition focus:border-accent/50 focus:ring-2 focus:ring-accent/20"
            placeholder="请输入密码"
          />
        </div>

        <Button type="submit" variant="accent" class="h-11 w-full rounded-xl" :disabled="!canSubmit">
          {{ loading ? '登录中...' : '登录' }}
        </Button>
      </form>

      <p class="mt-6 text-sm text-muted-foreground">
        还没有账号？
        <button
          type="button"
          class="font-medium text-accent hover:underline"
          @click="router.push({ name: 'register' })"
        >
          立即注册
        </button>
      </p>
    </div>
  </div>
</template>
