export default defineNuxtConfig({
  srcDir: 'app/',
  modules: ['@nuxt/ui'],
  devtools: { enabled: true },

  runtimeConfig: {
    public: {
      apiBase: 'http://localhost:8080'
    }
  }
})
