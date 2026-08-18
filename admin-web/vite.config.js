import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = process.env.VITE_BACKEND_URL || env.VITE_BACKEND_URL || 'http://localhost:8080'
  const proxy = {
    '/api': { target: backend, changeOrigin: true },
    '/uploads': { target: backend, changeOrigin: true }
  }
  return {
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      port: 5174,
      strictPort: true,
      proxy
    },
    preview: {
      host: '0.0.0.0',
      port: 5174,
      strictPort: true,
      proxy
    },
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: false,
      chunkSizeWarningLimit: 1700,
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            element: ['element-plus', '@element-plus/icons-vue'],
            charts: ['echarts'],
            http: ['axios']
          }
        }
      }
    }
  }
})
