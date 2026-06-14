import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  base: '/api/',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:10010',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: '../server/src/main/resources/static',
    emptyOutDir: true,
    sourcemap: false,
  },
})
