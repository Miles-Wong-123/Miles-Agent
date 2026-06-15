import { defineStore } from 'pinia'
import {
  HttpError,
  login as apiLogin,
  logout as apiLogout,
  me as apiMe,
  register as apiRegister,
  sendCode as apiSendCode,
  verifyCode as apiVerifyCode,
} from '@/lib/api'
import type { AuthUser } from '@/lib/types'

interface AuthState {
  user: AuthUser | null
  ready: boolean
}

let bootstrapPromise: Promise<void> | null = null

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    ready: false,
  }),
  actions: {
    async ensureReady() {
      if (this.ready) return
      if (!bootstrapPromise) {
        bootstrapPromise = this.fetchMe().finally(() => {
          bootstrapPromise = null
        })
      }
      await bootstrapPromise
    },

    async fetchMe() {
      try {
        this.user = await apiMe()
      } catch (error) {
        if (error instanceof HttpError && error.status === 401) {
          this.user = null
        } else {
          throw error
        }
      } finally {
        this.ready = true
      }
    },

    async sendCode(email: string) {
      return await apiSendCode(email)
    },

    async verifyCode(email: string, code: string) {
      return await apiVerifyCode(email, code)
    },

    async register(payload: { email: string; code: string; nickname: string; password: string }) {
      this.user = await apiRegister(payload)
      this.ready = true
      return this.user
    },

    async login(payload: { email: string; password: string }) {
      this.user = await apiLogin(payload)
      this.ready = true
      return this.user
    },

    async logout() {
      try {
        await apiLogout()
      } finally {
        this.user = null
        this.ready = true
      }
    },
  },
})
