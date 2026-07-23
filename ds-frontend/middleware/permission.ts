import { useAuthStore } from '~/stores/auth'

export default defineNuxtRouteMiddleware((to) => {
  const isAdminRoute = to.path.startsWith('/admin')
  const authStore = useAuthStore()
  authStore.initFromStorage(isAdminRoute ? 'admin' : 'member')

  const required = Array.from((to.meta?.permissions as string[] | undefined) || []) as string[]

  if (!required || required.length === 0) {
    console.log('[Perm MW] No permissions required for', to.path)
    return
  }

  if (!authStore.isLoggedIn) {
    console.log('[Perm MW] Not logged in, redirecting to /login')
    return navigateTo('/login')
  }

  const userPermissions = Array.from(authStore.user.permissions || []) as string[]
  console.log('[Perm MW] Required permissions for', to.path, ':', required)
  console.log('[Perm MW] User permissions:', userPermissions)

  const hasPermission = required.some((p) => userPermissions.includes(p))
  console.log('[Perm MW] Has permission:', hasPermission)

  if (!hasPermission) {
    console.log('[Perm MW] Permission denied, redirecting to /member')
    ElMessage.warning('无权访问该页面')
    return navigateTo('/member')
  }

  console.log('[Perm MW] Permission check passed for', to.path)
})
