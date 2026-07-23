import { useAuthStore } from '~/stores/auth'

export default defineNuxtPlugin(() => {
  if (import.meta.client) {
    const authStore = useAuthStore()
    const isAdmin = window.location.pathname.startsWith('/admin')
    authStore.initFromStorage(isAdmin ? 'admin' : 'member')
  }
})
