<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { HttpError } from '@/lib/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

const step = ref<1 | 2 | 3>(1)
const email = ref('')
const code = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)

const canSendCode = computed(() => email.value.trim().length > 0 && !loading.value)
const canVerifyCode = computed(() => code.value.trim().length === 6 && !loading.value)
const passwordsMatch = computed(() => password.value === confirmPassword.value)
const canRegister = computed(() => {
  return (
    nickname.value.trim().length > 0 &&
    password.value.length >= 8 &&
    confirmPassword.value.length > 0 &&
    passwordsMatch.value &&
    !loading.value
  )
})

async function handleSendCode() {
  if (!canSendCode.value) return
  loading.value = true
  try {
    await auth.sendCode(email.value.trim())
    toast.success('验证码已发送', { description: '请前往邮箱查看 6 位验证码。' })
    step.value = 2
  } catch (error) {
    const message = error instanceof HttpError ? error.message : '验证码发送失败'
    toast.error('发送失败', { description: message })
  } finally {
    loading.value = false
  }
}

async function handleVerifyCode() {
  if (!canVerifyCode.value) return
  loading.value = true
  try {
    await auth.verifyCode(email.value.trim(), code.value.trim())
    toast.success('验证码正确')
    step.value = 3
  } catch (error) {
    const message = error instanceof HttpError ? error.message : '验证码校验失败'
    toast.error('校验失败', { description: message })
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!canRegister.value) return
  loading.value = true
  try {
    await auth.register({
      email: email.value.trim(),
      code: code.value.trim(),
      nickname: nickname.value.trim(),
      password: password.value,
    })
    await router.push({ name: 'chat' })
  } catch (error) {
    const message = error instanceof HttpError ? error.message : '注册失败'
    toast.error('注册失败', { description: message })
    if (error instanceof HttpError && error.status === 400) {
      step.value = 2
    }
  } finally {
    loading.value = false
  }
}

function backTo(stepValue: 1 | 2) {
  step.value = stepValue
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-background px-4 py-10 text-foreground">
    <div class="w-full max-w-xl rounded-3xl border border-border bg-card/95 p-8 shadow-sm">
      <div class="space-y-2">
        <p class="text-sm text-muted-foreground">Miles-Agent</p>
        <h1 class="text-3xl font-semibold tracking-tight">邮箱注册</h1>
        <p class="text-sm leading-6 text-muted-foreground">完成注册后会自动登录并进入聊天页。</p>
      </div>

      <div class="mt-8 flex items-center gap-2 text-xs text-muted-foreground">
        <span :class="step >= 1 ? 'text-accent' : ''">1. 邮箱</span>
        <span>/</span>
        <span :class="step >= 2 ? 'text-accent' : ''">2. 验证码</span>
        <span>/</span>
        <span :class="step >= 3 ? 'text-accent' : ''">3. 昵称与密码</span>
      </div>

      <div v-if="step === 1" class="mt-6 space-y-4">
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground" for="register-email">邮箱</label>
          <input
            id="register-email"
            v-model="email"
            type="email"
            autocomplete="email"
            class="h-11 w-full rounded-xl border border-input bg-background px-4 text-sm text-foreground outline-none transition focus:border-accent/50 focus:ring-2 focus:ring-accent/20"
            placeholder="name@example.com"
          />
        </div>
        <Button type="button" variant="accent" class="h-11 w-full rounded-xl" :disabled="!canSendCode" @click="handleSendCode">
          {{ loading ? '发送中...' : '发送验证码' }}
        </Button>
      </div>

      <div v-else-if="step === 2" class="mt-6 space-y-4">
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground" for="register-code">验证码</label>
          <input
            id="register-code"
            v-model="code"
            type="text"
            inputmode="numeric"
            maxlength="6"
            class="h-11 w-full rounded-xl border border-input bg-background px-4 text-sm tracking-[0.4em] text-foreground outline-none transition focus:border-accent/50 focus:ring-2 focus:ring-accent/20"
            placeholder="6位验证码"
          />
        </div>
        <div class="flex gap-3">
          <Button type="button" variant="outline" class="h-11 flex-1 rounded-xl" @click="backTo(1)">返回修改邮箱</Button>
          <Button type="button" variant="accent" class="h-11 flex-1 rounded-xl" :disabled="!canVerifyCode" @click="handleVerifyCode">
            {{ loading ? '校验中...' : '下一步' }}
          </Button>
        </div>
      </div>

      <div v-else class="mt-6 space-y-4">
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground" for="register-nickname">昵称</label>
          <input
            id="register-nickname"
            v-model="nickname"
            type="text"
            maxlength="32"
            class="h-11 w-full rounded-xl border border-input bg-background px-4 text-sm text-foreground outline-none transition focus:border-accent/50 focus:ring-2 focus:ring-accent/20"
            placeholder="给自己起个名字"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground" for="register-password">密码</label>
          <input
            id="register-password"
            v-model="password"
            type="password"
            autocomplete="new-password"
            class="h-11 w-full rounded-xl border border-input bg-background px-4 text-sm text-foreground outline-none transition focus:border-accent/50 focus:ring-2 focus:ring-accent/20"
            placeholder="至少 8 位"
          />
        </div>
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground" for="register-confirm-password">确认密码</label>
          <input
            id="register-confirm-password"
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            class="h-11 w-full rounded-xl border border-input bg-background px-4 text-sm text-foreground outline-none transition focus:border-accent/50 focus:ring-2 focus:ring-accent/20"
            placeholder="再次输入密码"
          />
          <p v-if="confirmPassword && !passwordsMatch" class="text-xs text-destructive">两次输入的密码不一致。</p>
        </div>
        <div class="flex gap-3">
          <Button type="button" variant="outline" class="h-11 flex-1 rounded-xl" @click="backTo(2)">返回验证码</Button>
          <Button type="button" variant="accent" class="h-11 flex-1 rounded-xl" :disabled="!canRegister" @click="handleRegister">
            {{ loading ? '注册中...' : '注册并登录' }}
          </Button>
        </div>
      </div>

      <p class="mt-6 text-sm text-muted-foreground">
        已有账号？
        <button
          type="button"
          class="font-medium text-accent hover:underline"
          @click="router.push({ name: 'login' })"
        >
          返回登录
        </button>
      </p>
    </div>
  </div>
</template>
