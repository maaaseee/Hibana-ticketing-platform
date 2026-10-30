import { fileURLToPath, URL } from 'node:url'
import process from 'node:process'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

// Load environment variables for the current mode
const mode = process.env.NODE_ENV || 'development'
const env = loadEnv(mode, process.cwd(), '')

// Target Gateway URL: defaults to local Spring Cloud Gateway (port 8080).
// In production or Azure (Container Apps, Azure Functions, etc.), customize via VITE_GATEWAY_URL.
const gatewayTarget = env.VITE_GATEWAY_URL || env.GATEWAY_URL || process.env.VITE_GATEWAY_URL || 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tailwindcss(),
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // Unified API Gateway route for all microservices (Catalog, Inventory, Orders, etc.)
      '/api/v1': {
        target: gatewayTarget,
        changeOrigin: true,
        secure: false,
      },
      // WebSocket proxy for live chat through Gateway
      '/ws': {
        target: gatewayTarget.replace(/^http/, 'ws'),
        ws: true,
        changeOrigin: true,
      },
      // Legacy routes backward-compatibility
      '/api/catalog': {
        target: `${gatewayTarget}/api/v1`,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/catalog/, ''),
      },
      '/api/inventory': {
        target: `${gatewayTarget}/api/v1`,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/inventory/, ''),
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/radix-vue')) {
            return 'vendor-radix'
          }
          if (
            id.includes('node_modules/vue/') ||
            id.includes('node_modules/@vue/') ||
            id.includes('node_modules/vue-router/') ||
            id.includes('node_modules/pinia/')
          ) {
            return 'vendor-router-pinia'
          }
        },
      },
    },
  },
  preview: {
    port: 4173,
    proxy: {
      '/api/v1': {
        target: gatewayTarget,
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
