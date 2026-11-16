import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: { host: '127.0.0.1', port: 5173,
    proxy: { '/api': { target: 'http://127.0.0.1:18088', changeOrigin: false } }
  }
})
