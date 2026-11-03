import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const gatewayUrl = env.VITE_GATEWAY_URL || 'http://localhost:8080'
  const entraUrl = env.VITE_ENTRA_URL || 'http://localhost:4577'

  return {
    plugins: [
      tailwindcss(),
      vue(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      host: true,
      port: 5174,
      proxy: {
        // API Gateway for Catalog, Venues, Events, etc.
        '/api/v1': {
          target: gatewayUrl,
          changeOrigin: true,
          secure: false,
        },
        // Floci-AZ Microsoft Entra ID Token Endpoint proxy
        '/floci-entra': {
          target: entraUrl,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/floci-entra/, ''),
        },
      },
    },
  }
})
