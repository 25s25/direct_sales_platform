export default defineNuxtConfig({
  compatibilityDate: '2024-04-03',
  devtools: { enabled: true },
  modules: ['@pinia/nuxt', '@element-plus/nuxt'],
  imports: {
    dirs: ['stores'],
  },
  css: ['element-plus/dist/index.css', '~/assets/styles/main.scss'],
  vite: {
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    css: {
      preprocessorOptions: {
        scss: { additionalData: '' }
      }
    }
  },
  runtimeConfig: {
    public: {
      apiBase: 'http://localhost:8080'
    }
  },
  ssr: false,
  nitro: {
    preset: 'node-server'
  },
  experimental: {
    payloadExtraction: false
  },
  router: {
    options: {
      hashMode: false
    }
  }
})