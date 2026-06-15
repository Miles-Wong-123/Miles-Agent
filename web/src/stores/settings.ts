import { defineStore } from 'pinia'
import { v4 as uuidv4 } from 'uuid'

export type ThemeMode = 'light' | 'dark' | 'system'

interface SettingsState {
  sessionId: string
  useStreaming: boolean
  theme: ThemeMode
}

export const useSettingsStore = defineStore('settings', {
  state: (): SettingsState => ({
    sessionId: uuidv4(),
    useStreaming: true,
    theme: 'system',
  }),
  actions: {
    resetSession() {
      this.sessionId = uuidv4()
    },
  },
  persist: {
    key: 'miles.settings',
    storage: typeof window !== 'undefined' ? window.localStorage : undefined,
  },
})
