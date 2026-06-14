import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import { toast } from 'vue-sonner'

import App from './App.vue'
import { router } from './router'
import './styles/globals.css'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)

app.config.errorHandler = (err, _instance, info) => {
  // eslint-disable-next-line no-console
  console.error('[vue:error]', err, info)
  const message = err instanceof Error ? err.message : String(err)
  toast.error('应用错误', { description: message })
}

window.addEventListener('unhandledrejection', (event) => {
  // eslint-disable-next-line no-console
  console.error('[unhandledrejection]', event.reason)
  const reason = event.reason
  const message = reason instanceof Error ? reason.message : String(reason ?? '未知错误')
  toast.error('未处理的异常', { description: message })
})

app.mount('#app')
